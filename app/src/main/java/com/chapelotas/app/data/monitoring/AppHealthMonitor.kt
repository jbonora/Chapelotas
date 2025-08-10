package com.chapelotas.app.data.monitoring

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.getSystemService
import com.chapelotas.app.battery.HuaweiUtils
import com.chapelotas.app.data.notifications.ChapelotasNotificationService
import com.chapelotas.app.domain.debug.DebugLog
import com.chapelotas.app.domain.repositories.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

data class HealthStatus(
    val isHealthy: Boolean,
    val issues: List<HealthIssue>,
    val lastCheck: Long,
    val recommendations: List<String> = emptyList()
)

data class HealthIssue(
    val type: IssueType,
    val severity: IssueSeverity,
    val description: String,
    val recommendation: String? = null
)

enum class IssueType {
    SERVICE_NOT_RUNNING,
    HUAWEI_NOT_CONFIGURED,
    LONG_INACTIVITY,
    PERMISSION_MISSING,
    BATTERY_OPTIMIZATION_ENABLED,
    NOTIFICATION_CHANNELS_DISABLED,
    ALARM_PERMISSION_MISSING,
    NO_ACTIVE_TASKS,
    STORAGE_ISSUES,
    MEMORY_ISSUES
}

enum class IssueSeverity {
    CRITICAL,    // App won't work properly
    HIGH,        // Significantly impacts functionality
    MEDIUM,      // Some features may not work
    LOW          // Minor issues
}

@Singleton
class AppHealthMonitor @Inject constructor(
    private val context: Context,
    private val debugLog: DebugLog,
    private val taskRepository: TaskRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val prefs = context.getSharedPreferences("app_health", Context.MODE_PRIVATE)

    companion object {
        private const val INACTIVE_THRESHOLD_HOURS = 8
        private const val MEMORY_THRESHOLD_MB = 50
        private const val STORAGE_THRESHOLD_MB = 10
        private const val LAST_HEALTH_CHECK_KEY = "last_health_check"
        private const val HEALTH_CHECK_INTERVAL_MS = 30 * 60 * 1000L // 30 minutes
    }

    suspend fun checkAppHealth(): HealthStatus {
        return withContext(Dispatchers.IO) {
            val issues = mutableListOf<HealthIssue>()
            val recommendations = mutableListOf<String>()

            try {
                // Core functionality checks
                checkServiceHealth(issues)
                checkPermissions(issues)
                checkBatteryOptimization(issues)
                checkNotificationChannels(issues)
                checkAlarmPermissions(issues)

                // Device-specific checks
                if (HuaweiUtils.isHuaweiDevice()) {
                    checkHuaweiConfiguration(issues)
                }

                // App state checks
                checkAppActivity(issues)
                checkActiveTasks(issues)
                checkResourceUsage(issues)

                // Generate recommendations
                generateRecommendations(issues, recommendations)

                val isHealthy = issues.none { it.severity in listOf(IssueSeverity.CRITICAL, IssueSeverity.HIGH) }

                debugLog.add("🏥 HEALTH: Revisión completada. Issues: ${issues.size}, Healthy: $isHealthy")

                // Save health check timestamp
                saveHealthCheckTimestamp()

                HealthStatus(
                    isHealthy = isHealthy,
                    issues = issues,
                    lastCheck = System.currentTimeMillis(),
                    recommendations = recommendations
                )
            } catch (e: Exception) {
                debugLog.add("❌ HEALTH: Error durante revisión de salud: ${e.message}")

                HealthStatus(
                    isHealthy = false,
                    issues = listOf(
                        HealthIssue(
                            type = IssueType.STORAGE_ISSUES,
                            severity = IssueSeverity.HIGH,
                            description = "Error durante revisión de salud: ${e.message}",
                            recommendation = "Reinicia la aplicación"
                        )
                    ),
                    lastCheck = System.currentTimeMillis()
                )
            }
        }
    }

    private fun checkServiceHealth(issues: MutableList<HealthIssue>) {
        if (!isServiceRunning()) {
            issues.add(
                HealthIssue(
                    type = IssueType.SERVICE_NOT_RUNNING,
                    severity = IssueSeverity.CRITICAL,
                    description = "El servicio principal no está corriendo",
                    recommendation = "Reinicia la aplicación y verifica que los permisos estén habilitados"
                )
            )
        }
    }

    private fun checkPermissions(issues: MutableList<HealthIssue>) {
        val requiredPermissions = listOf(
            android.Manifest.permission.READ_CALENDAR,
            android.Manifest.permission.WAKE_LOCK,
            android.Manifest.permission.FOREGROUND_SERVICE,
            android.Manifest.permission.RECEIVE_BOOT_COMPLETED,
            android.Manifest.permission.VIBRATE
        )

        val missingPermissions = requiredPermissions.filter { permission ->
            context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            issues.add(
                HealthIssue(
                    type = IssueType.PERMISSION_MISSING,
                    severity = IssueSeverity.HIGH,
                    description = "Permisos faltantes: ${missingPermissions.joinToString(", ")}",
                    recommendation = "Ve a Configuración > Aplicaciones > Chapelotas > Permisos y habilita todos los permisos"
                )
            )
        }
    }

    private fun checkBatteryOptimization(issues: MutableList<HealthIssue>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService<PowerManager>()
            val packageName = context.packageName

            if (powerManager?.isIgnoringBatteryOptimizations(packageName) == false) {
                issues.add(
                    HealthIssue(
                        type = IssueType.BATTERY_OPTIMIZATION_ENABLED,
                        severity = IssueSeverity.HIGH,
                        description = "La optimización de batería está habilitada",
                        recommendation = "Ve a Configuración > Batería > Optimización de batería y excluye Chapelotas"
                    )
                )
            }
        }
    }

    private fun checkNotificationChannels(issues: MutableList<HealthIssue>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService<NotificationManager>()
            val channels = notificationManager?.notificationChannels

            val disabledChannels = channels?.filter {
                it.importance == NotificationManager.IMPORTANCE_NONE
            }?.map { it.id }

            if (!disabledChannels.isNullOrEmpty()) {
                issues.add(
                    HealthIssue(
                        type = IssueType.NOTIFICATION_CHANNELS_DISABLED,
                        severity = IssueSeverity.MEDIUM,
                        description = "Canales de notificación deshabilitados: ${disabledChannels.joinToString(", ")}",
                        recommendation = "Ve a Configuración > Aplicaciones > Chapelotas > Notificaciones y habilita todos los canales"
                    )
                )
            }
        }
    }

    private fun checkAlarmPermissions(issues: MutableList<HealthIssue>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService<AlarmManager>()

            if (alarmManager?.canScheduleExactAlarms() == false) {
                issues.add(
                    HealthIssue(
                        type = IssueType.ALARM_PERMISSION_MISSING,
                        severity = IssueSeverity.HIGH,
                        description = "Permiso para alarmas exactas no concedido",
                        recommendation = "Ve a Configuración > Aplicaciones > Chapelotas > Permisos especiales > Alarmas y recordatorios"
                    )
                )
            }
        }
    }

    private fun checkHuaweiConfiguration(issues: MutableList<HealthIssue>) {
        if (!isHuaweiConfigured()) {
            issues.add(
                HealthIssue(
                    type = IssueType.HUAWEI_NOT_CONFIGURED,
                    severity = IssueSeverity.HIGH,
                    description = "Configuración de Huawei incompleta",
                    recommendation = "Configura autoinicio y protección en segundo plano en la configuración de batería de Huawei"
                )
            )
        }
    }

    private fun checkAppActivity(issues: MutableList<HealthIssue>) {
        val lastActivity = getLastActivityTime()
        val hoursSinceLastActivity = (System.currentTimeMillis() - lastActivity) / (1000 * 60 * 60)

        if (hoursSinceLastActivity > INACTIVE_THRESHOLD_HOURS) {
            val severity = when {
                hoursSinceLastActivity > 24 -> IssueSeverity.HIGH
                hoursSinceLastActivity > 12 -> IssueSeverity.MEDIUM
                else -> IssueSeverity.LOW
            }

            issues.add(
                HealthIssue(
                    type = IssueType.LONG_INACTIVITY,
                    severity = severity,
                    description = "App inactiva por $hoursSinceLastActivity horas",
                    recommendation = "Abre la aplicación para verificar que todo funcione correctamente"
                )
            )
        }
    }

    private fun checkActiveTasks(issues: MutableList<HealthIssue>) {
        scope.launch {
            try {
                val activeTasks = taskRepository.observeAllTasks().first().filter { !it.isFinished }

                if (activeTasks.isEmpty()) {
                    issues.add(
                        HealthIssue(
                            type = IssueType.NO_ACTIVE_TASKS,
                            severity = IssueSeverity.LOW,
                            description = "No hay tareas activas",
                            recommendation = "Agrega algunas tareas o eventos para aprovechar la aplicación"
                        )
                    )
                } else {
                    // Check for tasks with scheduling issues
                    val tasksWithoutReminders = activeTasks.filter { it.nextReminderAt == null }
                    if (tasksWithoutReminders.isNotEmpty()) {
                        issues.add(
                            HealthIssue(
                                type = IssueType.STORAGE_ISSUES,
                                severity = IssueSeverity.MEDIUM,
                                description = "${tasksWithoutReminders.size} tareas sin recordatorios programados",
                                recommendation = "Reinicia la aplicación para reprogramar los recordatorios"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                debugLog.add("❌ HEALTH: Error verificando tareas activas: ${e.message}")
            }
        }
    }

    private fun checkResourceUsage(issues: MutableList<HealthIssue>) {
        // Check memory usage
        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memoryInfo)

        val availableMemoryMB = memoryInfo.availMem / (1024 * 1024)
        if (availableMemoryMB < MEMORY_THRESHOLD_MB) {
            issues.add(
                HealthIssue(
                    type = IssueType.MEMORY_ISSUES,
                    severity = IssueSeverity.MEDIUM,
                    description = "Memoria disponible baja: ${availableMemoryMB}MB",
                    recommendation = "Cierra otras aplicaciones para liberar memoria"
                )
            )
        }

        // Check storage space
        val internalDir = context.filesDir
        val freeSpaceMB = internalDir.freeSpace / (1024 * 1024)
        if (freeSpaceMB < STORAGE_THRESHOLD_MB) {
            issues.add(
                HealthIssue(
                    type = IssueType.STORAGE_ISSUES,
                    severity = IssueSeverity.HIGH,
                    description = "Espacio de almacenamiento bajo: ${freeSpaceMB}MB",
                    recommendation = "Libera espacio en el dispositivo eliminando archivos innecesarios"
                )
            )
        }
    }

    private fun generateRecommendations(issues: List<HealthIssue>, recommendations: MutableList<String>) {
        // Priority recommendations based on severity
        val criticalIssues = issues.filter { it.severity == IssueSeverity.CRITICAL }
        val highIssues = issues.filter { it.severity == IssueSeverity.HIGH }

        when {
            criticalIssues.isNotEmpty() -> {
                recommendations.add("🚨 CRÍTICO: Hay problemas graves que requieren atención inmediata")
                recommendations.addAll(criticalIssues.mapNotNull { it.recommendation })
            }
            highIssues.isNotEmpty() -> {
                recommendations.add("⚠️ IMPORTANTE: Hay problemas que pueden afectar el funcionamiento")
                recommendations.addAll(highIssues.mapNotNull { it.recommendation })
            }
            issues.isNotEmpty() -> {
                recommendations.add("ℹ️ INFO: Hay algunos problemas menores detectados")
            }
            else -> {
                recommendations.add("✅ Todo funciona correctamente")
            }
        }

        // Add general recommendations
        if (issues.any { it.type == IssueType.BATTERY_OPTIMIZATION_ENABLED }) {
            recommendations.add("💡 TIP: Desactivar la optimización de batería mejora la confiabilidad")
        }

        if (HuaweiUtils.isHuaweiDevice()) {
            recommendations.add("📱 HUAWEI: Asegúrate de configurar el autoinicio y protección en segundo plano")
        }
    }

    fun shouldPerformHealthCheck(): Boolean {
        val lastCheck = prefs.getLong(LAST_HEALTH_CHECK_KEY, 0L)
        val now = System.currentTimeMillis()
        return (now - lastCheck) > HEALTH_CHECK_INTERVAL_MS
    }

    private fun saveHealthCheckTimestamp() {
        prefs.edit()
            .putLong(LAST_HEALTH_CHECK_KEY, System.currentTimeMillis())
            .apply()
    }

    fun recordActivity() {
        context.getSharedPreferences("app_activity", Context.MODE_PRIVATE)
            .edit()
            .putLong("last_activity", System.currentTimeMillis())
            .apply()
        debugLog.add("📱 HEALTH: Actividad registrada")
    }

    fun getHealthSummary(): String {
        scope.launch {
            val health = checkAppHealth()
            val formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm")
            val checkTime = LocalDateTime.now().format(formatter)

            val summary = buildString {
                appendLine("🏥 SALUD DE LA APP - $checkTime")
                appendLine("Estado: ${if (health.isHealthy) "✅ Saludable" else "⚠️ Requiere atención"}")
                appendLine("Problemas detectados: ${health.issues.size}")

                if (health.issues.isNotEmpty()) {
                    appendLine("\n📋 PROBLEMAS:")
                    health.issues.forEach { issue ->
                        val icon = when (issue.severity) {
                            IssueSeverity.CRITICAL -> "🚨"
                            IssueSeverity.HIGH -> "⚠️"
                            IssueSeverity.MEDIUM -> "🟡"
                            IssueSeverity.LOW -> "ℹ️"
                        }
                        appendLine("$icon ${issue.description}")
                    }
                }

                if (health.recommendations.isNotEmpty()) {
                    appendLine("\n💡 RECOMENDACIONES:")
                    health.recommendations.forEach { rec ->
                        appendLine("• $rec")
                    }
                }
            }

            debugLog.add("📊 HEALTH: $summary")
        }

        return "Revisión de salud programada..."
    }

    private fun isServiceRunning(): Boolean {
        return try {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
                if (ChapelotasNotificationService::class.java.name == service.service.className) {
                    return true
                }
            }
            false
        } catch (e: Exception) {
            debugLog.add("❌ HEALTH: Error verificando servicio: ${e.message}")
            false
        }
    }

    private fun isHuaweiConfigured(): Boolean {
        val prefs = context.getSharedPreferences("battery_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("battery_configured", false)
    }

    private fun getLastActivityTime(): Long {
        val prefs = context.getSharedPreferences("app_activity", Context.MODE_PRIVATE)
        return prefs.getLong("last_activity", System.currentTimeMillis())
    }

    /**
     * Force a health check and return results immediately
     */
    suspend fun performImmediateHealthCheck(): HealthStatus {
        debugLog.add("🏥 HEALTH: Iniciando revisión inmediata de salud...")
        return checkAppHealth()
    }

    /**
     * Get a quick status without full health check
     */
    fun getQuickStatus(): String {
        val isServiceRunning = isServiceRunning()
        val lastActivity = getLastActivityTime()
        val hoursSinceActivity = (System.currentTimeMillis() - lastActivity) / (1000 * 60 * 60)

        return when {
            !isServiceRunning -> "🚨 Servicio no está corriendo"
            hoursSinceActivity > 24 -> "⚠️ Inactiva por ${hoursSinceActivity}h"
            hoursSinceActivity > 8 -> "🟡 Inactiva por ${hoursSinceActivity}h"
            else -> "✅ Funcionando bien"
        }
    }
}