package ir.k1adili.projectcam.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProjectEntity::class, PhotoEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun photoDao(): PhotoDao

    companion object {
        private const val DB_NAME = "projectcam.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    // No migration is written for the v1->v2 (added photos.heading_degrees)
                    // schema change yet - this app is still in early active development, so
                    // destructively recreating the DB on a schema bump is an acceptable
                    // trade-off versus crashing on startup. Anyone updating should use
                    // Settings -> "خروجی کامل (پشتیبان)" beforehand if they want to keep data;
                    // it can be restored afterwards via "بازیابی از پشتیبان".
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
