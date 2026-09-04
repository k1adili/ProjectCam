package ir.k1adili.projectcam.util

import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale

object ExifWriter {

    /**
     * Writes every piece of metadata ProjectCam captures into the file's EXIF tags (in addition
     * to the burned-in watermark), so the data survives even if the photo is cropped, re-exported,
     * or opened in tools that read EXIF instead of looking at the pixels.
     */
    fun write(
        file: File,
        capturedAt: LocalDateTime,
        latitude: Double?,
        longitude: Double?,
        accuracyMeters: Float?,
        headingDegrees: Float?,
        photographerName: String,
        projectName: String,
        note: String = ""
    ) {
        val exif = ExifInterface(file.absolutePath)

        val exifDateFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
        val dateString = exifDateFormat.format(Date.from(capturedAt.atZone(ZoneId.systemDefault()).toInstant()))
        exif.setAttribute(ExifInterface.TAG_DATETIME, dateString)
        exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, dateString)
        exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, dateString)

        if (latitude != null && longitude != null) {
            exif.setLatLong(latitude, longitude)
        }

        if (headingDegrees != null && !headingDegrees.isNaN()) {
            // "M" = magnetic north, matching what the rotation-vector sensor actually measures
            // (no magnetic-declination correction is applied to get a true-north bearing).
            exif.setAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION, headingDegrees.toString())
            exif.setAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION_REF, "M")
        }

        exif.setAttribute(ExifInterface.TAG_ARTIST, photographerName)
        exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, projectName)
        exif.setAttribute(ExifInterface.TAG_SOFTWARE, "ProjectCam")

        // Not every field ProjectCam tracks has a dedicated official EXIF tag (e.g. GPS accuracy
        // radius, the Persian direction label, the per-photo note) - fold those into a single
        // human-readable UserComment so they're still preserved in the file itself.
        val summary = buildString {
            append("پروژه: ").append(projectName)
            append(" | عکاس: ").append(photographerName)
            if (accuracyMeters != null && !accuracyMeters.isNaN()) {
                append(" | دقت GPS: ± ").append(accuracyMeters.toInt()).append("m")
            }
            if (headingDegrees != null && !headingDegrees.isNaN()) {
                append(" | جهت: ").append(CompassHelper.directionLabel(headingDegrees))
            }
            if (note.isNotBlank()) {
                append(" | یادداشت: ").append(note)
            }
        }
        exif.setAttribute(ExifInterface.TAG_USER_COMMENT, summary)

        exif.saveAttributes()
    }
}
