package ir.k1adili.projectcam.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("project_id")]
)
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "project_id")
    val projectId: Long,

    /**
     * File NAME only (e.g. "IMG_1699999999999.jpg"), NOT an absolute path - absolute paths break
     * across reinstalls/backups. Resolve to a real file via PhotoFileUtils.absoluteFile(context, fileName).
     */
    @ColumnInfo(name = "file_name")
    val fileName: String,

    @ColumnInfo(name = "latitude")
    val latitude: Double?,

    @ColumnInfo(name = "longitude")
    val longitude: Double?,

    /** GPS accuracy radius in meters at capture time, null if location could not be acquired. */
    @ColumnInfo(name = "accuracy_meters")
    val accuracyMeters: Float?,

    @ColumnInfo(name = "photographer_name")
    val photographerName: String,

    @ColumnInfo(name = "note")
    val note: String = "",

    @ColumnInfo(name = "captured_at_epoch_millis")
    val capturedAtEpochMillis: Long
)
