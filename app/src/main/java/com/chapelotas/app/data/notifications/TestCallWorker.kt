package com.chapelotas.app.data.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chapelotas.app.presentation.ui.CriticalAlertActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay

@HiltWorker
class TestCallWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val startTime = System.currentTimeMillis()
        Log.d("TestCall", "🟢 INICIO: Worker iniciado a las ${System.currentTimeMillis()}")

        return try {
            // OPCIÓN 1: Usar AlarmManager desde el principio (NO usar delay)
            val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Programar la llamada para EXACTAMENTE 10 segundos desde AHORA
            val callTime = System.currentTimeMillis() + 10000
            Log.d("TestCall", "⏰ Programando llamada para: $callTime (en 10 segundos)")

            val callIntent = Intent(appContext, CriticalAlertActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                        Intent.FLAG_FROM_BACKGROUND

                putExtra("message", "¡LLAMADA DE PRUEBA! Llegó exactamente 10 segundos después.")
                putExtra("event_id", "test_${System.currentTimeMillis()}")
                putExtra("notification_id", System.currentTimeMillis())
            }

            val callPending = PendingIntent.getActivity(
                appContext,
                8888,
                callIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Usar setAlarmClock para GARANTIZAR que se ejecute
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // AlarmClock tiene la máxima prioridad
                val alarmClockInfo = AlarmManager.AlarmClockInfo(
                    callTime,
                    callPending // Este es el que se ejecutará
                )
                alarmManager.setAlarmClock(alarmClockInfo, callPending)
                Log.d("TestCall", "✅ Alarma programada con setAlarmClock")
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    callTime,
                    callPending
                )
                Log.d("TestCall", "✅ Alarma programada con setExactAndAllowWhileIdle")
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    callTime,
                    callPending
                )
                Log.d("TestCall", "✅ Alarma programada con setExact")
            }

            // NO usar delay() - terminar el worker inmediatamente
            val endTime = System.currentTimeMillis()
            Log.d("TestCall", "🏁 FIN: Worker completado en ${endTime - startTime}ms")

            Result.success()

        } catch (e: Exception) {
            Log.e("TestCall", "❌ Error en TestCallWorker", e)
            Result.failure()
        }
    }
}