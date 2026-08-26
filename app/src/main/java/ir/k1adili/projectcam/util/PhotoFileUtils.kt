package ir.k1adili.projectcam.util

import android.content.Context
import android.os.Environment
import java.io.File

/**
 * Photos are stored in app-specific external storage (no runtime storage permission needed on
 * API 29+, and it's included in the FileProvider paths for sharing).
 *
 * IMPORTANT: [ir.k1adili.projectcam.data.local.PhotoEntity.fileName] stores only the file NAME,
 * never an absolute path. Absolute paths are tied to a specific app install / device and would
 * break after a backup restore, a reinstall, or a device change. Always resolve through
 * [absoluteFile] instead of persisting a full path.
 */
object PhotoFileUtils {

    fun photosDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "ProjectCam")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun exportsDir(context: Context): File {
        val dir = File(photosDir(context), "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun absoluteFile(context: Context, fileName: String): File =
        File(photosDir(context), fileName)

    fun newPhotoFileName(): String = "IMG_${System.currentTimeMillis()}.jpg"
}
