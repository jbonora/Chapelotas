package com.chapelotas.app.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

/**
 * Base de datos principal de Chapelotas V2
 * Por ahora solo tiene la tabla de tasks
 */
@Database(
    entities = [TaskEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TaskDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: TaskDatabase? = null

        fun getInstance(context: Context): TaskDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    "chapelotas_v2.db"
                )
                    .fallbackToDestructiveMigration() // Por ahora, en producción usar migraciones
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}