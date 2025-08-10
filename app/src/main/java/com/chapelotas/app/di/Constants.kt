package com.chapelotas.app.di

object Constants {

    // --- Notification Channels ---
    const val CHANNEL_ID_GENERAL = "chapelotas_general"
    const val CHANNEL_ID_CRITICAL = "chapelotas_critical"
    const val CHANNEL_ID_SERVICE = "chapelotas_service_channel"
    const val CHANNEL_ID_HEARTBEAT = "chapelotas_heartbeat"
    // --- CANALES DE INSISTENCIA SIMPLIFICADOS ---
    const val CHANNEL_ID_INSISTENCE_NORMAL = "chapelotas_insistence_normal"
    const val CHANNEL_ID_INSISTENCE_MEDIUM = "chapelotas_insistence_medium"
    const val CHANNEL_ID_INSISTENCE_LOW = "chapelotas_insistence_low"

    // --- Notification IDs ---
    const val NOTIFICATION_ID_FOREGROUND_SERVICE = 1337
    const val NOTIFICATION_ID_HEARTBEAT = 111

    // --- Intent Actions ---
    const val ACTION_START_MONITORING = "com.chapelotas.action.START_MONITORING"
    const val ACTION_TRIGGER_REMINDER = "com.chapelotas.action.TRIGGER_REMINDER"
    const val ACTION_ACKNOWLEDGE = "com.chapelotas.action.ACKNOWLEDGE"
    const val ACTION_FINISH_DONE = "com.chapelotas.action.FINISH_DONE"

    // --- Intent Extras ---
    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_EVENT_ID = "event_id"
    const val EXTRA_ANDROID_NOTIF_ID = "android_notif_id"
    const val EXTRA_NOTIFICATION_MESSAGE = "message"
    const val EXTRA_NOTIFICATION_ID = "notification_id"
    const val EXTRA_TASK_ID_TO_HIGHLIGHT = "extra_task_id_to_highlight"

    // --- PendingIntent Request Codes ---
    const val REQUEST_CODE_HEARTBEAT = 999
    const val REQUEST_CODE_OPEN_APP = 1000
    const val REQUEST_CODE_CRITICAL_ALERT = 2000
}