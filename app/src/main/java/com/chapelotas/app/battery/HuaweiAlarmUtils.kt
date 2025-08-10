package com.chapelotas.app.battery

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.chapelotas.app.R

object HuaweiUtils {

    private const val HUAWEI_SYSTEM_MANAGER = "com.huawei.systemmanager"

    private val LAUNCH_MANAGER_INTENTS = listOf(
        Intent().apply {
            component = ComponentName(
                HUAWEI_SYSTEM_MANAGER,
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            )
        },
        Intent().apply {
            component = ComponentName(
                HUAWEI_SYSTEM_MANAGER,
                "com.huawei.systemmanager.optimize.process.ProtectActivity"
            )
        },
        Intent().apply {
            component = ComponentName(
                HUAWEI_SYSTEM_MANAGER,
                "com.chapelotas.app.data.notifications.HuaweiOnboardingManager"
            )
        }
    )

    private val BATTERY_OPTIMIZATION_INTENT = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    } else null

    fun isHuaweiDevice(): Boolean {
        return Build.MANUFACTURER.equals("huawei", ignoreCase = true) ||
                Build.MANUFACTURER.equals("honor", ignoreCase = true)
    }

    // --- FUNCIONES AÑADIDAS ---

    fun getHuaweiVersion(): String {
        return try {
            val emuiVersion = getSystemProperty("ro.build.version.emui")
            val harmonyVersion = getSystemProperty("hw_sc.build.platform.version")
            harmonyVersion ?: emuiVersion ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    fun isHarmonyOS(): Boolean {
        return getSystemProperty("hw_sc.build.platform.version") != null
    }

    private fun getSystemProperty(key: String): String? {
        return try {
            val process = Runtime.getRuntime().exec("getprop $key")
            process.inputStream.bufferedReader().use { it.readText().trim() }
        } catch (e: Exception) {
            null
        }
    }

    // --- FIN DE FUNCIONES AÑADIDAS ---

    fun showHuaweiOptimizationDialog(context: Context) {
        val isHarmony4 = isHarmonyOS4()

        val options = mutableListOf<String>()
        options += if (isHarmony4) "1. Configurar Launch Manager" else "1. Configurar App Launch"
        options += "2. Configurar optimización de batería"
        options += "3. Permitir ventana flotante (Dropzone)"

        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.huawei_optimization_title))
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> openLaunchManagerSettings(context, isHarmony4)
                    1 -> openBatteryOptimizationSettings(context)
                    2 -> requestDropzonePermission(context)
                }
            }
            .setNegativeButton(context.getString(R.string.later), null)
            .show()
    }

    private fun openLaunchManagerSettings(context: Context, isHarmony4: Boolean) {
        for (intent in LAUNCH_MANAGER_INTENTS) {
            try {
                context.startActivity(intent)
                val instructions = if (isHarmony4) {
                    context.getString(R.string.harmony4_launch_manager_instructions)
                } else {
                    context.getString(R.string.huawei_app_launch_instructions)
                }
                Toast.makeText(context, instructions, Toast.LENGTH_LONG).show()
                return
            } catch (_: Exception) {}
        }
        openAppDetailsSettings(context)
    }

    private fun openBatteryOptimizationSettings(context: Context) {
        try {
            BATTERY_OPTIMIZATION_INTENT?.let {
                context.startActivity(it)
                Toast.makeText(
                    context,
                    context.getString(R.string.battery_optimization_instructions),
                    Toast.LENGTH_LONG
                ).show()
            } ?: openAppDetailsSettings(context)
        } catch (e: Exception) {
            openAppDetailsSettings(context)
        }
    }

    private fun openAppDetailsSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
            Toast.makeText(
                context,
                context.getString(R.string.huawei_manual_instructions),
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.settings_error),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun hasDropzonePermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun requestDropzonePermission(context: Context) {
        if (!hasDropzonePermission(context)) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
                Toast.makeText(
                    context,
                    context.getString(R.string.dropzone_instructions),
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    context.getString(R.string.dropzone_unavailable),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.dropzone_already_granted),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun showPowerGenieWarning(context: Context) {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.powergenie_warning_title))
            .setMessage(context.getString(R.string.powergenie_warning_message))
            .setPositiveButton(context.getString(R.string.understood), null)
            .show()
    }

    fun needsBatteryOptimization(context: Context): Boolean {
        val prefs = context.getSharedPreferences("battery_prefs", Context.MODE_PRIVATE)
        return !prefs.getBoolean("battery_configured", false)
    }

    fun markBatteryOptimizationConfigured(context: Context) {
        context.getSharedPreferences("battery_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("battery_configured", true)
            .apply()
    }

    // Agregué la función isHarmonyOS4() que faltaba para evitar errores de compilación
    fun isHarmonyOS4(): Boolean {
        return try {
            val harmonyVersion = getSystemProperty("hw_sc.build.platform.version")
            harmonyVersion?.startsWith("4") == true
        } catch (e: Exception) {
            false
        }
    }
}