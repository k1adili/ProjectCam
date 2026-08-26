package ir.k1adili.projectcam.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "note")
    val note: String = "",

    /** Epoch millis (UTC) of creation, used only for sorting/backup - display always goes through JalaliDateUtils. */
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long
)
