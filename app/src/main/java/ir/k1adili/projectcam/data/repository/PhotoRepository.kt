package ir.k1adili.projectcam.data.repository

import android.content.Context
import ir.k1adili.projectcam.data.local.PhotoDao
import ir.k1adili.projectcam.data.local.PhotoEntity
import ir.k1adili.projectcam.util.PhotoFileUtils
import kotlinx.coroutines.flow.Flow

class PhotoRepository(
    private val context: Context,
    private val photoDao: PhotoDao
) {

    fun observePhotos(projectId: Long): Flow<List<PhotoEntity>> = photoDao.observeByProject(projectId)

    fun observePhoto(photoId: Long): Flow<PhotoEntity?> = photoDao.observeById(photoId)

    suspend fun getPhotosByIds(photoIds: List<Long>): List<PhotoEntity> {
        if (photoIds.isEmpty()) return emptyList()
        return photoDao.getByIds(photoIds)
    }

    suspend fun getPhotosByProjectOnce(projectId: Long): List<PhotoEntity> =
        photoDao.getByProjectOnce(projectId)

    suspend fun getAllPhotosOnce(): List<PhotoEntity> = photoDao.getAllOnce()

    fun absoluteFile(photo: PhotoEntity) = PhotoFileUtils.absoluteFile(context, photo.fileName)

    suspend fun addPhoto(
        projectId: Long,
        fileName: String,
        latitude: Double?,
        longitude: Double?,
        accuracyMeters: Float?,
        headingDegrees: Float?,
        photographerName: String,
        note: String = ""
    ): Long = photoDao.insert(
        PhotoEntity(
            projectId = projectId,
            fileName = fileName,
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = accuracyMeters,
            headingDegrees = headingDegrees,
            photographerName = photographerName,
            note = note,
            capturedAtEpochMillis = System.currentTimeMillis()
        )
    )

    suspend fun updateNote(photo: PhotoEntity, note: String) {
        photoDao.update(photo.copy(note = note))
    }

    /** Deletes the DB rows and their backing files. Missing files are ignored (already gone). */
    suspend fun deletePhotos(photos: List<PhotoEntity>) {
        if (photos.isEmpty()) return
        photoDao.deleteByIds(photos.map { it.id })
        for (photo in photos) {
            runCatching { absoluteFile(photo).delete() }
        }
    }
}
