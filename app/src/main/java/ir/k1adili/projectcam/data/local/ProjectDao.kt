package ir.k1adili.projectcam.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class ProjectWithPhotoCount(
    val id: Long,
    val name: String,
    val note: String,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
    val photoCount: Int
)

@Dao
interface ProjectDao {

    @Query("SELECT * FROM projects ORDER BY created_at_epoch_millis DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query(
        """
        SELECT p.id AS id, p.name AS name, p.note AS note, p.created_at_epoch_millis AS created_at_epoch_millis,
               (SELECT COUNT(*) FROM photos ph WHERE ph.project_id = p.id) AS photoCount
        FROM projects p
        ORDER BY p.created_at_epoch_millis DESC
        """
    )
    fun observeAllWithPhotoCount(): Flow<List<ProjectWithPhotoCount>>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun observeById(projectId: Long): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getById(projectId: Long): ProjectEntity?

    @Query("SELECT * FROM projects ORDER BY created_at_epoch_millis DESC")
    suspend fun getAllOnce(): List<ProjectEntity>

    @Insert
    suspend fun insert(project: ProjectEntity): Long

    @Update
    suspend fun update(project: ProjectEntity)

    @Delete
    suspend fun delete(project: ProjectEntity)

    @Query("SELECT COUNT(*) FROM photos WHERE project_id = :projectId")
    fun observePhotoCount(projectId: Long): Flow<Int>

    @Query("DELETE FROM projects")
    suspend fun deleteAll()

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteById(projectId: Long)
}
