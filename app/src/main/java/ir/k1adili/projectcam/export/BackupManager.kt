package ir.k1adili.projectcam.export

import android.content.Context
import ir.k1adili.projectcam.data.local.AppDatabase
import ir.k1adili.projectcam.data.local.PhotoEntity
import ir.k1adili.projectcam.data.local.ProjectEntity
import ir.k1adili.projectcam.util.PhotoFileUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * The device's Auto Backup (see data_extraction_rules.xml / backup_rules.xml) is a lightweight
 * safety net but is subject to OS/OEM quotas and doesn't give the user something they can move
 * to a new phone by hand. This is the explicit, user-triggered full export/import that the
 * project conventions require in addition to Auto Backup.
 */
object BackupManager {

    private const val MANIFEST_ENTRY = "manifest.json"
    private const val MANIFEST_VERSION = 1

    data class BackupSummary(val projectCount: Int, val photoCount: Int)

    suspend fun exportBackup(context: Context, database: AppDatabase, outputFile: File): BackupSummary {
        val projects = database.projectDao().getAllOnce()
        val photos = database.photoDao().getAllOnce()

        outputFile.parentFile?.mkdirs()
        ZipOutputStream(FileOutputStream(outputFile)).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(buildManifest(projects, photos).toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            for (photo in photos) {
                val source = PhotoFileUtils.absoluteFile(context, photo.fileName)
                if (!source.exists()) continue
                zip.putNextEntry(ZipEntry("photos/${photo.fileName}"))
                FileInputStream(source).use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return BackupSummary(projects.size, photos.size)
    }

    private fun buildManifest(projects: List<ProjectEntity>, photos: List<PhotoEntity>): JSONObject {
        val root = JSONObject()
        root.put("version", MANIFEST_VERSION)
        root.put("exportedAtEpochMillis", System.currentTimeMillis())

        val projectsArray = JSONArray()
        for (p in projects) {
            projectsArray.put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("note", p.note)
                    .put("createdAtEpochMillis", p.createdAtEpochMillis)
            )
        }
        root.put("projects", projectsArray)

        val photosArray = JSONArray()
        for (ph in photos) {
            photosArray.put(
                JSONObject()
                    .put("id", ph.id)
                    .put("projectId", ph.projectId)
                    .put("fileName", ph.fileName)
                    .put("latitude", ph.latitude ?: JSONObject.NULL)
                    .put("longitude", ph.longitude ?: JSONObject.NULL)
                    .put("accuracyMeters", if (ph.accuracyMeters != null && !ph.accuracyMeters.isNaN()) ph.accuracyMeters.toDouble() else JSONObject.NULL)
                    .put("photographerName", ph.photographerName)
                    .put("note", ph.note)
                    .put("capturedAtEpochMillis", ph.capturedAtEpochMillis)
            )
        }
        root.put("photos", photosArray)
        return root
    }

    /**
     * Replaces ALL current projects/photos with the contents of [inputFile]. Callers must confirm
     * this destructive action with the user before calling.
     */
    suspend fun importBackup(context: Context, database: AppDatabase, inputFile: File): BackupSummary {
        var manifest: JSONObject? = null
        val extractedFiles = mutableMapOf<String, ByteArray>()

        ZipInputStream(FileInputStream(inputFile)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                val bytes = zip.readBytes()
                if (name == MANIFEST_ENTRY) {
                    manifest = JSONObject(String(bytes, Charsets.UTF_8))
                } else if (name.startsWith("photos/")) {
                    extractedFiles[name.removePrefix("photos/")] = bytes
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val root = manifest ?: throw IllegalArgumentException("فایل پشتیبان معتبر نیست")

        // Wipe current data: delete existing photo files, then clear DB (cascades photos).
        val existingPhotos = database.photoDao().getAllOnce()
        for (photo in existingPhotos) {
            runCatching { PhotoFileUtils.absoluteFile(context, photo.fileName).delete() }
        }
        database.projectDao().deleteAll()

        // Insert projects, remembering old-id -> new-id since PKs are auto-generated fresh.
        val projectsJson = root.getJSONArray("projects")
        val oldToNewProjectId = HashMap<Long, Long>()
        for (i in 0 until projectsJson.length()) {
            val obj = projectsJson.getJSONObject(i)
            val oldId = obj.getLong("id")
            val newId = database.projectDao().insert(
                ProjectEntity(
                    name = obj.getString("name"),
                    note = obj.optString("note", ""),
                    createdAtEpochMillis = obj.getLong("createdAtEpochMillis")
                )
            )
            oldToNewProjectId[oldId] = newId
        }

        val photosDir = PhotoFileUtils.photosDir(context)
        val photosJson = root.getJSONArray("photos")
        var importedPhotoCount = 0
        for (i in 0 until photosJson.length()) {
            val obj = photosJson.getJSONObject(i)
            val oldProjectId = obj.getLong("projectId")
            val newProjectId = oldToNewProjectId[oldProjectId] ?: continue
            val fileName = obj.getString("fileName")

            val bytes = extractedFiles[fileName]
            if (bytes != null) {
                File(photosDir, fileName).writeBytes(bytes)
            }

            database.photoDao().insert(
                PhotoEntity(
                    projectId = newProjectId,
                    fileName = fileName,
                    latitude = obj.optDoubleOrNull("latitude"),
                    longitude = obj.optDoubleOrNull("longitude"),
                    accuracyMeters = obj.optDoubleOrNull("accuracyMeters")?.toFloat(),
                    photographerName = obj.optString("photographerName", ""),
                    note = obj.optString("note", ""),
                    capturedAtEpochMillis = obj.getLong("capturedAtEpochMillis")
                )
            )
            importedPhotoCount++
        }

        return BackupSummary(oldToNewProjectId.size, importedPhotoCount)
    }

    private fun JSONObject.optDoubleOrNull(key: String): Double? =
        if (isNull(key)) null else optDouble(key)
}
