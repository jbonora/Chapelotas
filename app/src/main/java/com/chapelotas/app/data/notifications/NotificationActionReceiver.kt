package com.chapelotas.app.data.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
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

        // Acciones que los botones de la notificación pueden enviar
        const val ACTION_ACKNOWLEDGE = "com.chapelotas.action.ACKNOWLEDGE"
        const val ACTION_FINISH_DONE = "com.chapelotas.action.FINISH_DONE"

        // Claves para los datos extra que viajan en el Intent
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_ANDROID_NOTIF_ID = "android_notif_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: ""
        val androidNotifId = intent.getIntExtra(EXTRA_ANDROID_NOTIF_ID, 0)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (eventId.isEmpty()) {
            Log.e(TAG, "❌ Error: Se recibió una acción sin un ID de evento.")
            return
        }

        // goAsync() le dice al sistema: "¡Esperá! Estoy haciendo algo importante en segundo plano.
        // Dame un poco más de tiempo antes de matarme".
        val pendingResult = goAsync()

        // Se lanza una corutina para poder interactuar con la base de datos
        scope.launch {
            try {
                Log.d(TAG, "📱 Acción recibida: ${intent.action} para el evento $eventId")
                when (intent.action) {
                    ACTION_ACKNOWLEDGE -> {
                        Log.d(TAG, "👍 Acción: Marcar como ENTENDIDO el evento $eventId")
                        taskRepository.acknowledgeTask(eventId)
                    }
                    ACTION_FINISH_DONE -> {
                        Log.d(TAG, "✅ Acción: Marcar como TERMINADO el evento $eventId")
                        taskRepository.finishTask(eventId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando la acción '${intent.action}' para el evento $eventId", e)
            } finally {
                // Independientemente del resultado, se cancela la notificación para que no quede visible
                if (androidNotifId != 0) {
                    notificationManager.cancel(androidNotifId)
                }
                // Le avisamos al sistema que ya terminamos nuestro trabajo.
                pendingResult.finish()
                Log.d(TAG, "✅ Acción procesada y finalizada para el evento $eventId")
            }
        }
    }
}