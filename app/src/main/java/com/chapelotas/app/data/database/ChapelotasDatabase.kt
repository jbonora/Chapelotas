package com.chapelotas.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.chapelotas.app.data.database.converters.Converters
import com.chapelotas.app.data.database.daos.*
import com.chapelotas.app.data.database.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [
        DayPlan::class,
        EventPlan::class,
        ScheduledNotification::class,
        EventConflict::class,
        NotificationLog::class,
        ConversationLog::class,
        MonkeyAgenda::class,
        ChatThread::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ChapelotasDatabase : RoomDatabase() {

    abstract fun dayPlanDao(): DayPlanDao
    abstract fun eventPlanDao(): EventPlanDao
    abstract fun notificationDao(): NotificationDao
    abstract fun conflictDao(): ConflictDao
    abstract fun notificationLogDao(): NotificationLogDao
    abstract fun conversationLogDao(): ConversationLogDao
    abstract fun monkeyAgendaDao(): MonkeyAgendaDao
    abstract fun chatThreadDao(): ChatThreadDao

    companion object {
        @Volatile
        private var INSTANCE: ChapelotasDatabase? = null

        fun getDatabase(
            context: Context,
            scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
        ): ChapelotasDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChapelotasDatabase::class.java,
                    "chapelotas_database"
                )
                    .fallbackToDestructiveMigration() // Si algo falla, recrear la BD
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}