package ir.k1adili.projectcam.data.repository

import ir.k1adili.projectcam.data.local.ProjectDao
import ir.k1adili.projectcam.data.local.ProjectEntity
import ir.k1adili.projectcam.data.local.ProjectWithPhotoCount
import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val projectDao: ProjectDao) {

    fun observeProjects(): Flow<List<ProjectEntity>> = projectDao.observeAll()

    fun observeProjectsWithPhotoCount(): Flow<List<ProjectWithPhotoCount>> =
        projectDao.observeAllWithPhotoCount()

    fun observeProject(projectId: Long): Flow<ProjectEntity?> = projectDao.observeById(projectId)

    fun observePhotoCount(projectId: Long): Flow<Int> = projectDao.observePhotoCount(projectId)

    suspend fun getProject(projectId: Long): ProjectEntity? = projectDao.getById(projectId)

    suspend fun getAllProjectsOnce(): List<ProjectEntity> = projectDao.getAllOnce()

    suspend fun createProject(name: String, note: String): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Project name must not be empty" }
        return projectDao.insert(
            ProjectEntity(
                name = trimmed,
                note = note.trim(),
                createdAtEpochMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun renameProject(project: ProjectEntity, newName: String, newNote: String) {
        val trimmed = newName.trim()
        require(trimmed.isNotEmpty()) { "Project name must not be empty" }
        projectDao.update(project.copy(name = trimmed, note = newNote.trim()))
    }

    suspend fun deleteProject(project: ProjectEntity) {
        projectDao.delete(project)
    }

    suspend fun deleteProjectById(projectId: Long) {
        projectDao.deleteById(projectId)
    }
}
