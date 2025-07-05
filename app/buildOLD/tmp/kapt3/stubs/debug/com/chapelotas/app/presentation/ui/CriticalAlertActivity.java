package com.chapelotas.app.presentation.ui;

/**
 * Actividad de alerta crítica que simula una llamada entrante
 * No se puede ignorar fácilmente
 */
@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u0007\u001a\u00020\b2\b\u0010\t\u001a\u0004\u0018\u00010\nH\u0014J\b\u0010\u000b\u001a\u00020\bH\u0014J\b\u0010\f\u001a\u00020\bH\u0002J\b\u0010\r\u001a\u00020\bH\u0002R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0005\u001a\u0004\u0018\u00010\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000e"}, d2 = {"Lcom/chapelotas/app/presentation/ui/CriticalAlertActivity;", "Landroidx/activity/ComponentActivity;", "()V", "ringtone", "Landroid/media/Ringtone;", "vibrator", "Landroid/os/Vibrator;", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "onDestroy", "startAlarm", "stopAlarm", "app_debug"})
public final class CriticalAlertActivity extends androidx.activity.ComponentActivity {
    @org.jetbrains.annotations.Nullable()
    private android.media.Ringtone ringtone;
    @org.jetbrains.annotations.Nullable()
    private android.os.Vibrator vibrator;
    
    public CriticalAlertActivity() {
        super();
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    @java.lang.Override()
    protected void onDestroy() {
    }
    
    private final void startAlarm() {
    }
    
    private final void stopAlarm() {
    }
}