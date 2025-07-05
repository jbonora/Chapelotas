package com.chapelotas.app.domain.entities;

/**
 * Tipo de notificación según su propósito
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\b\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005j\u0002\b\u0006j\u0002\b\u0007j\u0002\b\b\u00a8\u0006\t"}, d2 = {"Lcom/chapelotas/app/domain/entities/NotificationType;", "", "(Ljava/lang/String;I)V", "DAILY_SUMMARY", "TOMORROW_SUMMARY", "EVENT_REMINDER", "CRITICAL_ALERT", "PREPARATION_TIP", "SARCASTIC_NUDGE", "app_debug"})
public enum NotificationType {
    /*public static final*/ DAILY_SUMMARY /* = new DAILY_SUMMARY() */,
    /*public static final*/ TOMORROW_SUMMARY /* = new TOMORROW_SUMMARY() */,
    /*public static final*/ EVENT_REMINDER /* = new EVENT_REMINDER() */,
    /*public static final*/ CRITICAL_ALERT /* = new CRITICAL_ALERT() */,
    /*public static final*/ PREPARATION_TIP /* = new PREPARATION_TIP() */,
    /*public static final*/ SARCASTIC_NUDGE /* = new SARCASTIC_NUDGE() */;
    
    NotificationType() {
    }
    
    @org.jetbrains.annotations.NotNull()
    public static kotlin.enums.EnumEntries<com.chapelotas.app.domain.entities.NotificationType> getEntries() {
        return null;
    }
}