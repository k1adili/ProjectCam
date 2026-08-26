package ir.k1adili.projectcam.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ir.k1adili.projectcam.ui.screens.camera.CameraScreen
import ir.k1adili.projectcam.ui.screens.photo.PhotoViewerScreen
import ir.k1adili.projectcam.ui.screens.projects.ProjectDetailScreen
import ir.k1adili.projectcam.ui.screens.projects.ProjectListScreen
import ir.k1adili.projectcam.ui.screens.settings.SettingsScreen

private object Routes {
    const val PROJECT_LIST = "projects"
    const val PROJECT_DETAIL = "project/{projectId}"
    const val CAMERA = "camera/{projectId}"
    const val PHOTO_VIEWER = "photo/{photoId}"
    const val SETTINGS = "settings"

    fun projectDetail(projectId: Long) = "project/$projectId"
    fun camera(projectId: Long) = "camera/$projectId"
    fun photoViewer(photoId: Long) = "photo/$photoId"
}

@Composable
fun ProjectCamNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.PROJECT_LIST) {
        composable(Routes.PROJECT_LIST) {
            ProjectListScreen(
                onOpenProject = { navController.navigate(Routes.projectDetail(it)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(
            Routes.PROJECT_DETAIL,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
            ProjectDetailScreen(
                projectId = projectId,
                onBack = { navController.popBackStack() },
                onOpenCamera = { navController.navigate(Routes.camera(it)) },
                onOpenPhoto = { navController.navigate(Routes.photoViewer(it)) }
            )
        }

        composable(
            Routes.CAMERA,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
            CameraScreen(
                projectId = projectId,
                onBack = { navController.popBackStack() },
                onPhotoSaved = { navController.popBackStack() }
            )
        }

        composable(
            Routes.PHOTO_VIEWER,
            arguments = listOf(navArgument("photoId") { type = NavType.LongType })
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getLong("photoId") ?: return@composable
            PhotoViewerScreen(
                photoId = photoId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
