package com.chapelotas.app.battery

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import com.chapelotas.app.R
import java.util.concurrent.TimeUnit

class HuaweiOnboardingManager(private val context: Context) {

    fun shouldShowOnboarding(): Boolean {
        val prefs = context.getSharedPreferences("huawei_setup", Context.MODE_PRIVATE)
        return HuaweiUtils.isHuaweiDevice() &&
                !prefs.getBoolean("setup_completed", false)
    }

    fun showOnboardingFlow(activity: Activity) {
        if (!shouldShowOnboarding()) return

        // Mostrar diálogo que NO se puede cancelar hasta configurar
        AlertDialog.Builder(activity)
            .setTitle("⚠️ Configuración Crítica para Huawei")
            .setMessage("""
                Tu dispositivo Huawei puede matar esta app durante la noche.
                
                Para que las alarmas funcionen correctamente, debes configurar 3 ajustes críticos:
                
                1. Launch Manager (Gestión de inicio)
                2. Optimización de batería 
                3. Permisos de ventana flotante
                
                Sin esto, perderás recordatorios importantes.
            """.trimIndent())
            .setCancelable(false) // No se puede cancelar
            .setPositiveButton("Configurar ahora") { _, _ ->
                showStepByStepGuide(activity)
            }
            .setNegativeButton("Recordármelo después") { _, _ ->
                // Programar recordatorio en 1 hora
                scheduleReminder()
            }
            .show()
    }

    private fun showStepByStepGuide(activity: Activity) {
        val steps = listOf(
            "Paso 1: Launch Manager",
            "Paso 2: Optimización de batería",
            "Paso 3: Ventana flotante"
        )

        fun showStep(stepIndex: Int) {
            if (stepIndex >= steps.size) {
                // Configuración completa
                markSetupCompleted()
                showCompletionDialog(activity)
                return
            }

            AlertDialog.Builder(activity)
                .setTitle("${steps[stepIndex]} (${stepIndex + 1}/${steps.size})")
                .setMessage(getStepInstructions(stepIndex))
                .setPositiveButton("Abrir configuración") { _, _ ->
                    when (stepIndex) {
                        0 -> HuaweiUtils.showHuaweiOptimizationDialog(activity)
                        1 -> BatteryProtectionManager.requestIgnoreBatteryOptimizations(activity)
                        2 -> HuaweiUtils.requestDropzonePermission(activity)
                    }
                }
                .setNeutralButton("Siguiente paso") { _, _ ->
                    showStep(stepIndex + 1)
                }
                .setNegativeButton("Cancelar") { _, _ ->
                    // No marcar como completado
                }
                .show()
        }

        showStep(0)
    }

    private fun getStepInstructions(stepIndex: Int): String {
        return when (stepIndex) {
            0 -> """
                1. Se abrirá "Launch Manager" 
                2. Busca "Chapelotas"
                3. Cambia de "Automático" a "Manual"
                4. Activa los 3 toggles:
                   • Inicio automático ✅
                   • Inicio secundario ✅ 
                   • Ejecución en segundo plano ✅
            """.trimIndent()

            1 -> """
                1. Se abrirá "Optimización de batería"
                2. Busca "Chapelotas" 
                3. Selecciona "No optimizar"
                4. Confirma los cambios
            """.trimIndent()

            2 -> """
                1. Se abrirá "Permisos de superposición"
                2. Busca "Chapelotas"
                3. Activa "Permitir mostrar sobre otras apps"
                4. Confirma los cambios
            """.trimIndent()

            else -> ""
        }
    }

    private fun markSetupCompleted() {
        context.getSharedPreferences("huawei_setup", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("setup_completed", true)
            .putLong("setup_date", System.currentTimeMillis())
            .apply()
    }

    private fun showCompletionDialog(activity: Activity) {
        AlertDialog.Builder(activity)
            .setTitle("✅ Configuración Completada")
            .setMessage("¡Perfecto! Ahora Chapelotas podrá funcionar correctamente en tu dispositivo Huawei.")
            .setPositiveButton("Entendido", null)
            .show()
    }

    private fun scheduleReminder() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, HuaweiSetupReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1234, // Request code único para esta alarma
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Programar para dentro de 1 hora
        val triggerAtMillis = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // Manejar el caso en que no se tengan permisos, aunque es poco probable aquí.
        }
    }
}