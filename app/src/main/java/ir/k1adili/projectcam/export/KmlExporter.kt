package ir.k1adili.projectcam.export

import ir.k1adili.projectcam.data.local.PhotoEntity
import ir.k1adili.projectcam.data.local.ProjectEntity
import ir.k1adili.projectcam.util.JalaliDateUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object KmlExporter {

    /**
     * Builds a ZIP file at [outputFile] containing:
     *  - doc.kml at the archive root, with one Placemark per photo that has a GPS fix
     *  - files/<name> for every photo passed in (including ones without GPS, so nothing
     *    the user selected silently disappears from the export)
     *
     * [resolveFile] resolves each PhotoEntity to its actual on-disk File (callers own the
     * context needed to do this - see PhotoRepository.absoluteFile).
     *
     * Photos without coordinates are skipped in the KML itself (a Placemark needs a Point)
     * but are still included in files/ and counted in the returned [ExportResult].
     */
    fun export(
        project: ProjectEntity,
        photos: List<PhotoEntity>,
        outputFile: File,
        resolveFile: (PhotoEntity) -> File
    ): ExportResult {
        val photosWithLocation = photos.filter { it.latitude != null && it.longitude != null }
        val skippedCount = photos.size - photosWithLocation.size

        outputFile.parentFile?.mkdirs()

        // Compute each photo's archive entry name exactly once so the KML <img src> and the
        // actual zip entry can never disagree.
        val entryNameForPhotoId = HashMap<Long, String>()
        val usedNames = HashSet<String>()
        for ((index, photo) in photos.withIndex()) {
            val source = resolveFile(photo)
            val baseName = sanitizeFileName(source.name).ifBlank { "photo_$index.jpg" }
            var fileName = baseName
            var suffix = 1
            while (!usedNames.add(fileName)) {
                fileName = "${index}_${suffix}_$baseName"
                suffix++
            }
            entryNameForPhotoId[photo.id] = fileName
        }

        ZipOutputStream(FileOutputStream(outputFile)).use { zip ->
            zip.putNextEntry(ZipEntry("doc.kml"))
            zip.write(buildKml(project, photos, entryNameForPhotoId).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            for (photo in photos) {
                val source = resolveFile(photo)
                if (!source.exists()) continue
                val fileName = entryNameForPhotoId[photo.id] ?: continue
                zip.putNextEntry(ZipEntry("files/$fileName"))
                FileInputStream(source).use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
        }

        return ExportResult(
            outputFile = outputFile,
            placemarkCount = photosWithLocation.size,
            skippedNoLocationCount = skippedCount
        )
    }

    private fun buildKml(
        project: ProjectEntity,
        photos: List<PhotoEntity>,
        entryNameForPhotoId: Map<Long, String>
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append('\n')
        sb.append("""<kml xmlns="http://www.opengis.net/kml/2.2">""").append('\n')
        sb.append("<Document>\n")
        sb.append("<name>").append(xmlEscape(project.name)).append("</name>\n")

        for ((index, photo) in photos.withIndex()) {
            if (photo.latitude == null || photo.longitude == null) continue
            val fileName = entryNameForPhotoId[photo.id] ?: continue

            val capturedAt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(photo.capturedAtEpochMillis),
                ZoneId.systemDefault()
            )
            val dateText = JalaliDateUtils.formatDateTimeNumeric(capturedAt)
            val description = buildString {
                append("عکاس: ").append(xmlEscape(photo.photographerName)).append("<br/>")
                append("تاریخ: ").append(xmlEscape(dateText)).append("<br/>")
                if (photo.note.isNotBlank()) {
                    append("توضیح: ").append(xmlEscape(photo.note)).append("<br/>")
                }
                photo.accuracyMeters?.takeIf { !it.isNaN() }?.let {
                    append("دقت GPS: ").append(xmlEscape("± ${it.toInt()} m")).append("<br/>")
                }
                append("""<img src="files/$fileName" width="400"/>""")
            }

            sb.append("<Placemark>\n")
            sb.append("<name>").append(xmlEscape("عکس ${index + 1}")).append("</name>\n")
            sb.append("<description><![CDATA[").append(description).append("]]></description>\n")
            sb.append("<Point><coordinates>")
                .append(photo.longitude).append(',').append(photo.latitude).append(",0")
                .append("</coordinates></Point>\n")
            sb.append("</Placemark>\n")
        }

        sb.append("</Document>\n")
        sb.append("</kml>\n")
        return sb.toString()
    }

    private fun xmlEscape(input: String): String = input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun sanitizeFileName(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._-]"), "_")

    data class ExportResult(
        val outputFile: File,
        val placemarkCount: Int,
        val skippedNoLocationCount: Int
    )
}
