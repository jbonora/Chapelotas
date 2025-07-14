# 🏗️ Chapelotas - Arquitectura Quick Reference

## 🎯 **PRINCIPIO FUNDAMENTAL**
**MonkeyAgenda = Única fuente de verdad para acciones del mono**

```
ESCRITORES → MonkeyAgenda → MonkeyAgendaService → Usuario
(múltiples)    (tabla BD)      (procesador)       (ve/oye)
```

## 🔄 **FLUJOS PRINCIPALES**

### 1. Flujo de Inicio de App
```
MainActivity
    ↓
CalendarMonitorViewModel.iniciarDia()
    ↓
Detecta tipo de inicio (primera vez, force stop, nuevo día)
    ↓
MigrateToMonkeyAgendaUseCase (si necesario)
    ↓
CalendarSyncUseCase → Eventos del calendario
    ↓
UnifiedMonkeyService.scheduleNextAlarm()
    ↓
Espera máximo 1 hora
```

### 2. Flujo de Notificación
```
AlarmManager dispara
    ↓
NotificationAlarmReceiver
    ↓
UnifiedMonkeyService.processNotificationsAndReschedule()
    ↓
MonkeyAgendaService.processNextAction()
    ↓
Procesa UNA acción (marca COMPLETED)
    ↓
Genera mensaje con AI
    ↓
Guarda en ConversationLog (con threadId)
    ↓
Muestra notificación
    ↓
scheduleNextAlarm() → máximo 1 hora
```

### 3. Flujo de Evento Crítico
```
Evento marcado como crítico
    ↓
UnifiedMonkeyService (bypasses MonkeyAgenda)
    ↓
Programa alarmas directas con AlarmManager
    ↓
CriticalAlertActivity (pantalla completa + sonido)
```

## 📊 **ESTRUCTURA DE DATOS**

### Tablas Principales
- **day_plans** - Config del día (modo 24h, sarcástico)
- **event_plans** - Eventos del calendario procesados
- **monkey_agenda** - Cola de acciones a ejecutar ⭐
- **chat_threads** - Threads de conversación
- **conversation_logs** - Mensajes del chat

### Relaciones Clave
```
DayPlan (1) → (N) EventPlan
EventPlan (1) → (N) MonkeyAgenda
EventPlan (1) → (1) ChatThread
ChatThread (1) → (N) ConversationLog
```

## 🛡️ **PROTECCIONES ANTI-LOOP**

1. **MonkeyAgenda**: Una acción = Un proceso = COMPLETED
2. **Máximo 3 notificaciones** por ciclo
3. **Alarma mínima 1 minuto** en el futuro
4. **Acciones >2h antiguas** → Resumen único
5. **Estado en DB** no en memoria

## 🔧 **CONFIGURACIÓN CLAVE**

### Modos de Operación
- **Modo 24h**: `DayPlan.is24hMode`
- **Modo Sarcástico**: `DayPlan.sarcasticMode`
- **Horario Laboral**: `workStartTime` - `workEndTime`

### Tipos de Acciones (MonkeyAgenda)
- **NOTIFY_EVENT** - Recordatorio de evento
- **IDLE_CHECK** - Chequeo de inactividad (sarcástico)
- **DAILY_SUMMARY** - Resumen diario
- **CLEANUP** - Limpieza semanal

### Estados de Thread
- **ACTIVE** - En progreso
- **COMPLETED** - Terminado
- **MISSED** - Perdido
- **ARCHIVED** - Archivado

## 🐛 **GOTCHAS Y SOLUCIONES**

### Problema: "Bombardeo de 50 mensajes"
**Causa**: Procesar todas las notificaciones viejas individualmente
**Solución**: Detectar si hay acciones >2h y generar resumen

### Problema: "776 minutos hasta próxima"
**Causa**: Solo programaba si no había NADA pendiente
**Solución**: Siempre programar máximo 1 hora, crear IDLE_CHECK intermedio

### Problema: "Force Stop pierde alarmas"
**Causa**: Android cancela alarmas al force stop
**Solución**: Detectar con `areAlarmsConfigured()` y reconfigurar

### Problema: "Eventos pasados siguen PENDING"
**Causa**: No se actualizaba el estado automáticamente
**Solución**: `refreshEventStates()` al iniciar

## 📝 **PARA RETOMAR DESARROLLO**

### Verificar Estado Actual
```sql
-- Ver próximas acciones
SELECT * FROM monkey_agenda 
WHERE status = 'PENDING' 
ORDER BY scheduled_time;

-- Ver threads activos
SELECT * FROM chat_threads 
WHERE status = 'ACTIVE';

-- Verificar configuración
SELECT * FROM day_plans 
WHERE date = date('now');
```

### Debug Rápido
1. ¿El mono dice tiempo absurdo? → Ver `scheduleNextAlarm()`
2. ¿No molesta por inactividad? → Verificar modo sarcástico + 24h
3. ¿Bombardea mensajes? → Ver threshold en `processNextAction()`
4. ¿No se recupera de force stop? → Check `iniciarDia()` detection

### Próximo Feature Prioritario
**Vista Chat tipo WhatsApp** - Los threads ya existen, solo falta UI:
1. `ChatListFragment` - Lista de ChatThread
2. `ChatDetailFragment` - ConversationLog por threadId
3. `SendMessageUseCase` - Procesar input usuario

## 🚀 **COMANDOS ÚTILES**

```bash
# Ver logs del mono
adb logcat | grep -E "MonkeyAgenda|UnifiedMonkey|CalendarMonitor"

# Forzar proceso de alarma
adb shell am broadcast -a com.chapelotas.PROCESS_NOTIFICATIONS

# Limpiar BD y empezar fresh
adb shell pm clear com.chapelotas.app
```

## 💡 **DECISIONES DE DISEÑO**

1. **¿Por qué MonkeyAgenda?** → Evitar loops, una fuente de verdad
2. **¿Por qué máximo 1 hora?** → Balance entre batería y responsividad  
3. **¿Por qué threads separados?** → Contexto por evento, como WhatsApp
4. **¿Por qué AI para mensajes?** → Variedad, personalización, contexto
5. **¿Por qué Room + DataStore?** → Room para datos, DataStore para prefs

---

**Remember**: El mono es hincha pelotas por diseño, pero controlado 🐵