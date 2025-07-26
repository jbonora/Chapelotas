package com.chapelotas.app.battery // <-- CAMBIO IMPORTANTE

import android.content.*
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog // Importante para el diálogo
import com.chapelotas.app.R // Importante para los textos

object BatteryProtectionManager {

    fun showBatteryProtectionDialog(context: Context, onDismiss: () -> Unit) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.battery_protection_title)
        builder.setMessage(R.string.battery_protection_message)
        builder.setPositiveButton(R.string.battery_protection_positive) { _, _ ->
            requestIgnoreBatteryOptimizations(context)
            openAutoStartSettings(context)
            openHuaweiProtectedApps(context)
            onDismiss()
        }
        builder.setNegativeButton(R.string.battery_protection_negative) { _, _ ->
            onDismiss()
        }
        builder.show()
    }

    private fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = context.packageName
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.data = Uri.parse("package:$packageName")
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun openAutoStartSettings(context: Context) {
        val intent = when {
            Build.MANUFACTURER.equals("huawei", ignoreCase = true) -> Intent().apply {
                component = ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
            }
            Build.MANUFACTURER.equals("xiaomi", ignoreCase = true) -> Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            }
            Build.MANUFACTURER.equals("oppo", ignoreCase = true) -> Intent().apply {
                component = ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )
            }
            Build.MANUFACTURER.equals("vivo", ignoreCase = true) -> Intent().apply {
                component = ComponentName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                )
            }
            else -> null
        }

        try {
            intent?.let {
                context.startActivity(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openHuaweiProtectedApps(context: Context) {
        if (Build.MANUFACTURER.equals("huawei", ignoreCase = true)) {
            try {
                val intent = Intent()
                intent.setClassName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity"
                )
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}