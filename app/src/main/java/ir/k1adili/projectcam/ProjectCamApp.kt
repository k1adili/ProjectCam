package ir.k1adili.projectcam

import android.app.Application
import ir.k1adili.projectcam.data.SettingsRepository
import ir.k1adili.projectcam.data.local.AppDatabase
import ir.k1adili.projectcam.data.repository.PhotoRepository
import ir.k1adili.projectcam.data.repository.ProjectRepository

class ProjectCamApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val projectRepository: ProjectRepository by lazy { ProjectRepository(database.projectDao()) }
    val photoRepository: PhotoRepository by lazy { PhotoRepository(this, database.photoDao()) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
}
