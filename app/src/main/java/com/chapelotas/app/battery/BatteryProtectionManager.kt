package com.chapelotas.app.battery

import android.content.*
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.chapelotas.app.R

object BatteryProtectionManager {

    private const val HUAWEI_SYSTEM_MANAGER = "com.huawei.systemmanager"

    /**
     * Muestra el diálogo inicial para configurar la batería
     */
    fun showBatteryProtectionDialog(context: Context, onDismiss: () -> Unit = {}) {
        AlertDialog.Builder(context)
            .setTitle(R.string.battery_protection_title)
            .setMessage(R.string.battery_protection_message)
            .setPositiveButton(R.string.battery_protection_positive) { _, _ ->
                // Mostrar opciones según el fabricante
                when {
                    isHuaweiDevice() -> showHuaweiOptionsDialog(context, onDismiss)
                    else -> {
                        // Para otros dispositivos, solo la optimización estándar
                        requestIgnoreBatteryOptimizations(context)
                        onDismiss()
                    }
                }
            }
            .setNegativeButton(R.string.battery_protection_negative) { _, _ ->
                onDismiss()
            }
            .show()
    }

    /**
     * Diálogo específico para Huawei con opciones separadas
     */
    private fun showHuaweiOptionsDialog(context: Context, onComplete: () -> Unit) {
        val options = arrayOf(
            "1. Launch Manager (Inicio de apps)",
            "2. Optimización de batería estándar"
        )

        var completedSteps = BooleanArray(2)

        AlertDialog.Builder(context)
            .setTitle("Configuración Huawei - Elige una opción")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        openLaunchManagerSettings(context)
                        completedSteps[0] = true
                    }
                    1 -> {
                        requestIgnoreBatteryOptimizations(context)
                        completedSteps[1] = true
                    }
                }

                // Si ambos pasos están completos, ejecutar callback
                if (completedSteps.all { it }) {
                    onComplete()
                } else {
                    // Volver a mostrar el diálogo para la siguiente opción
                    showHuaweiOptionsDialog(context, onComplete)
                }
            }
            .setNegativeButton("Cancelar") { _, _ ->
                onComplete()
            }
            .show()
    }

    /**
     * Verifica si es un dispositivo Huawei/Honor
     */
    private fun isHuaweiDevice(): Boolean {
        return Build.MANUFACTURER.equals("huawei", ignoreCase = true) ||
                Build.MANUFACTURER.equals("honor", ignoreCase = true)
    }

    /**
     * Abre la configuración de Launch Manager en Huawei
     */
    private fun openLaunchManagerSettings(context: Context) {
        val intents = listOf(
            // HarmonyOS 4.x / EMUI 9+
            Intent().apply {
                component = ComponentName(
                    HUAWEI_SYSTEM_MANAGER,
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
            },
            // EMUI 5-8 (fallback)
            Intent().apply {
                component = ComponentName(
                    HUAWEI_SYSTEM_MANAGER,
                    "com.huawei.systemmanager.optimize.process.ProtectActivity"
                )
            }
        )

        var opened = false
        for (intent in intents) {
            try {
                context.startActivity(intent)
                opened = true
                showLaunchManagerInstructions(context)
                break
            } catch (e: Exception) {
                // Intentar con el siguiente
            }
        }

        if (!opened) {
            Toast.makeText(
                context,
                "No se pudo abrir Launch Manager. Ve a Configuración → Batería → Launch Manager",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Muestra instrucciones para Launch Manager
     */
    private fun showLaunchManagerInstructions(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Instrucciones Launch Manager")
            .setMessage("""
                1. Busca "${context.getString(R.string.app_name)}"
                2. Cambia de "Automático" a "Manual"
                3. Activa los 3 toggles:
                   • Inicio automático
                   • Inicio secundario
                   • Ejecución en segundo plano
                   
                Presiona OK cuando hayas terminado.
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    /**
     * Solicita ignorar optimizaciones de batería (Android estándar)
     */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = context.packageName

            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback a la configuración general
                    try {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        context.startActivity(intent)
                    } catch (e2: Exception) {
                        Toast.makeText(
                            context,
                            "No se pudo abrir la configuración de batería",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                Toast.makeText(
                    context,
                    "La optimización de batería ya está desactivada",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Verifica si la app está excluida de optimizaciones de batería
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true // En versiones antiguas no hay restricciones
        }
    }
}