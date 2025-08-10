package com.chapelotas.app.data.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.chapelotas.app.di.Constants
import com.chapelotas.app.domain.repositories.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var taskRepository: TaskRepository
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "NotificationReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getStringExtra(Constants.EXTRA_EVENT_ID) ?: ""
        val androidNotifId = intent.getIntExtra(Constants.EXTRA_ANDROID_NOTIF_ID, 0)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (eventId.isEmpty()) {
            Log.e(TAG, "❌ Error: Se recibió una acción sin un ID de evento.")
            return
        }

        val pendingResult = goAsync()

        scope.launch {
            try {
                Log.d(TAG, "📱 Acción recibida: ${intent.action} para el evento $eventId")
                when (intent.action) {
                    Constants.ACTION_ACKNOWLEDGE -> {
                        Log.d(TAG, "👍 Acción: Marcar como ENTENDIDO el evento $eventId")
                        taskRepository.acknowledgeTask(eventId)
                    }
                    Constants.ACTION_FINISH_DONE -> {
                        Log.d(TAG, "✅ Acción: Marcar como TERMINADO el evento $eventId")
                        taskRepository.finishTask(eventId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando la acción '${intent.action}' para el evento $eventId", e)
            } finally {
                if (androidNotifId != 0) {
                    notificationManager.cancel(androidNotifId)
                }
                pendingResult.finish()
                Log.d(TAG, "✅ Acción procesada y finalizada para el evento $eventId")
            }
        }
    }
}