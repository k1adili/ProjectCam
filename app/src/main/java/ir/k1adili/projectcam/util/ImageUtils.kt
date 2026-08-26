package ir.k1adili.projectcam.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

object ImageUtils {

    /** Decodes [file] and bakes its EXIF rotation into the pixel data (so downstream code never has to think about orientation again). */
    fun decodeUprightBitmap(file: File): Bitmap {
        val original = BitmapFactory.decodeFile(file.absolutePath)
            ?: throw IllegalStateException("Could not decode captured image")

        val exif = ExifInterface(file.absolutePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        }

        return if (matrix.isIdentity) {
            original
        } else {
            val rotated = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
            if (rotated !== original) original.recycle()
            rotated
        }
    }

    fun saveJpeg(bitmap: Bitmap, destination: File, quality: Int = 92) {
        destination.parentFile?.mkdirs()
        FileOutputStream(destination).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
    }
}
