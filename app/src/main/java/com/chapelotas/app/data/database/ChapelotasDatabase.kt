package com.chapelotas.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.chapelotas.app.data.database.converters.Converters
import com.chapelotas.app.data.database.daos.*
import com.chapelotas.app.data.database.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Base de datos principal de Chapelotas
 * Single source of truth para toda la app
 */
@Database(
    entities = [
        DayPlan::class,
        EventPlan::class,
        ScheduledNotification::class,
        EventConflict::class,
        NotificationLog::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ChapelotasDatabase : RoomDatabase() {

    // DAOs
    abstract fun dayPlanDao(): DayPlanDao
    abstract fun eventPlanDao(): EventPlanDao
    abstract fun notificationDao(): NotificationDao
    abstract fun conflictDao(): ConflictDao
    abstract fun notificationLogDao(): NotificationLogDao

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
                    // Callback para crear data inicial si es necesario
                    .addCallback(DatabaseCallback(scope))
                    // Permitir queries en main thread solo para debugging
                    // .allowMainThreadQueries() // NO usar en producción
                    // Habilitar destructive migration durante desarrollo
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }

        /**
         * Callback para operaciones al crear/abrir la DB
         */
        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Poblar con datos iniciales si es necesario
                INSTANCE?.let { database ->
                    scope.launch {
                        populateInitialData(database)
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Limpiar datos viejos al abrir
                INSTANCE?.let { database ->
                    scope.launch {
                        cleanOldData(database)
                    }
                }
            }
        }

        /**
         * Datos iniciales (si son necesarios)
         */
        suspend fun populateInitialData(database: ChapelotasDatabase) {
            // Por ahora no necesitamos datos iniciales
            // Pero aquí podrías crear un DayPlan para hoy, etc.
        }

        /**
         * Limpieza automática de datos viejos
         */
        suspend fun cleanOldData(database: ChapelotasDatabase) {
            try {
                // Borrar planes de más de 30 días
                val thirtyDaysAgo = LocalDate.now().minusDays(30)
                database.dayPlanDao().deleteOldPlans(thirtyDaysAgo)
                database.eventPlanDao().deleteOldEvents(thirtyDaysAgo)

                // Borrar notificaciones ejecutadas de más de 7 días
                val sevenDaysAgo = LocalDateTime.now().minusDays(7)
                database.notificationDao().deleteOldExecuted(sevenDaysAgo)

                // Borrar conflictos resueltos de más de 14 días
                val fourteenDaysAgo = LocalDateTime.now().minusDays(14)
                database.conflictDao().deleteOldResolved(fourteenDaysAgo)

                // Borrar logs de más de 90 días (para analytics)
                val ninetyDaysAgo = LocalDateTime.now().minusDays(90)
                database.notificationLogDao().deleteOldLogs(ninetyDaysAgo)

            } catch (e: Exception) {
                // Log error but don't crash
                e.printStackTrace()
            }
        }

        /**
         * Método útil para tests - NO usar en producción
         */
        fun destroyInstance() {
            INSTANCE = null
        }
    }

    /**
     * Transacciones útiles
     */

    /**
     * Crea o actualiza un plan completo para un evento
     */

}