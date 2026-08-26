package ir.k1adili.projectcam.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {

    @Query("SELECT * FROM photos WHERE project_id = :projectId ORDER BY captured_at_epoch_millis DESC")
    fun observeByProject(projectId: Long): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE id = :photoId")
    fun observeById(photoId: Long): Flow<PhotoEntity?>

    @Query("SELECT * FROM photos WHERE id IN (:photoIds)")
    suspend fun getByIds(photoIds: List<Long>): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE project_id = :projectId ORDER BY captured_at_epoch_millis DESC")
    suspend fun getByProjectOnce(projectId: Long): List<PhotoEntity>

    @Query("SELECT * FROM photos")
    suspend fun getAllOnce(): List<PhotoEntity>

    @Insert
    suspend fun insert(photo: PhotoEntity): Long

    @Update
    suspend fun update(photo: PhotoEntity)

    @Delete
    suspend fun delete(photo: PhotoEntity)

    @Query("DELETE FROM photos WHERE id IN (:photoIds)")
    suspend fun deleteByIds(photoIds: List<Long>)
}
