package com.chapelotas.app.domain.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Representa el estado actual de un permiso o configuración.
 */
enum class PermissionStatus {
    GRANTED, // Concedido
    DENIED,  // Denegado
    UNKNOWN  // No aplicable o desconocido
}

/**
 * Contiene un informe completo del estado de los permisos de la app.
 */
data class AppStatusReport(
    val calendarPermission: PermissionStatus,
    val notificationPermission: PermissionStatus,
    val isBatteryOptimizationDisabled: PermissionStatus,
    val canDrawOverlays: PermissionStatus
)

/**
 * Clase centralizada para verificar el estado de los permisos y configuraciones
 * críticas de la aplicación. Es el único responsable de esta lógica.
 */
@Singleton
class AppStatusManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Genera un informe completo con el estado actual de todos los permisos importantes.
     */
    fun getStatusReport(): AppStatusReport {
        return AppStatusReport(
            calendarPermission = hasCalendarPermission(),
            notificationPermission = hasNotificationPermission(),
            isBatteryOptimizationDisabled = isBatteryOptimizationDisabled(),
            canDrawOverlays = canDrawOverlays()
        )
    }

    /**
     * Verifica si el permiso para leer el calendario ha sido concedido.
     */
    fun hasCalendarPermission(): PermissionStatus {
        return if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            PermissionStatus.GRANTED
        } else {
            PermissionStatus.DENIED
        }
    }

    /**
     * Verifica si el permiso para enviar notificaciones ha sido concedido.
     */
    fun hasNotificationPermission(): PermissionStatus {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                PermissionStatus.GRANTED
            } else {
                PermissionStatus.DENIED
            }
        }
        // Para versiones más antiguas, se verifica si el usuario las ha deshabilitado manualmente.
        return if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            PermissionStatus.GRANTED
        } else {
            PermissionStatus.DENIED
        }
    }

    /**
     * Verifica si la app ha sido excluida de las optimizaciones de batería del sistema.
     */
    fun isBatteryOptimizationDisabled(): PermissionStatus {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return if (pm.isIgnoringBatteryOptimizations(context.packageName)) {
                PermissionStatus.GRANTED
            } else {
                PermissionStatus.DENIED
            }
        }
        return PermissionStatus.UNKNOWN // No aplicable en versiones antiguas
    }

    /**
     * Verifica si la app tiene permiso para "dibujar sobre otras aplicaciones".
     */
    fun canDrawOverlays(): PermissionStatus {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return if (Settings.canDrawOverlays(context)) {
                PermissionStatus.GRANTED
            } else {
                PermissionStatus.DENIED
            }
        }
        return PermissionStatus.GRANTED // Concedido por defecto en versiones antiguas
    }
}