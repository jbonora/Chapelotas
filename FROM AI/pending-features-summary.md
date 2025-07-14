# 🚧 Funcionalidades Pendientes de Implementar

## 📱 **UI TIPO WHATSAPP**

### Vista de Lista de Chats
- **ChatListScreen** - Lista de threads como WhatsApp
  - Thread "General" siempre arriba
  - Threads de eventos ordenados por hora
  - Badge de mensajes no leídos
  - Preview del último mensaje
  - Swipe para archivar threads completados

### Vista de Chat Individual
- **ChatDetailScreen** - Conversación completa
  - Mensajes del mono (izquierda)
  - Respuestas del usuario (derecha)
  - Input para escribir respuestas
  - Botones de acción rápida ("Hecho", "Snooze", "Cancelar")
  - Header con info del evento

### Componentes UI faltantes
- **ThreadListItem** - Item individual en la lista
- **MessageBubble** - Burbuja de mensaje con timestamp
- **QuickActionBar** - Barra de acciones rápidas
- **ChatInputBar** - Input con botón de enviar

## 🤖 **LÓGICA DE INTERACCIÓN**

### Sistema de Respuestas del Usuario
- **ProcessUserResponseUseCase** - Procesar mensajes del usuario
- Interpretar comandos ("listo", "hecho", "cancelar", etc.)
- Actualizar EventResolutionStatus según respuesta
- Generar respuesta contextual del mono

### Comandos de Voz
- Integración con reconocimiento de voz
- Comandos rápidos por voz
- Transcripción en el chat

## 📊 **FUNCIONALIDADES AVANZADAS**

### Resumen Diario Completo
```kotlin
suspend fun processDailySummary() {
    // Generar resumen del día
    // Estadísticas de productividad
    // Eventos completados vs pendientes
    // Sugerencias para mañana
}
```

### Estadísticas y Analytics
- **ProductivityStatsScreen** - Dashboard de productividad
- Eventos completados a tiempo
- Tiempo promedio de respuesta
- Patrones de procrastinación
- Gráficos de actividad

### Configuración Avanzada
- **WorkScheduleScreen** - Configurar horarios por día
- Diferentes horarios lun-vie vs fin de semana
- Días de vacaciones/feriados
- Modo "No molestar" temporal

## 🔧 **MEJORAS TÉCNICAS**

### MonkeyAgenda UI
- **AgendaDebugScreen** - Ver/editar MonkeyAgenda
- Lista de acciones pendientes
- Poder cancelar/reprogramar acciones
- Logs de ejecución

### Sistema de Plantillas
- Mensajes personalizables por tipo
- Plantillas según personalidad del usuario
- Diferentes tonos (formal/casual/sarcástico extremo)

### Integración con Calendario
- Sincronización bidireccional
- Crear eventos desde la app
- Modificar eventos del calendario

## 🎨 **PERSONALIZACIÓN**

### Temas y Avatares
- Diferentes avatares para Chapelotas
- Temas de color personalizables
- Modo oscuro/claro
- Sonidos personalizados por tipo de notificación

### Perfiles de Secretaria
- "Motivadora" - Mensajes positivos
- "Estricta" - Sin excusas
- "Amigable" - Tono casual
- "Profesional" - Formal empresarial

## 🔔 **NOTIFICACIONES AVANZADAS**

### Rich Notifications
- Acciones directas desde notificación
- Preview expandible
- Respuesta rápida sin abrir app

### Smart Notifications
- Agrupar por contexto
- Prioridad dinámica según historial
- Modo "resumen" para menos interrupciones

## 🔗 **INTEGRACIONES**

### Asistentes de Voz
- Google Assistant
- Alexa
- Acciones por voz

### Wearables
- Wear OS app companion
- Notificaciones en smartwatch
- Acciones rápidas desde reloj

### Widgets
- Widget de agenda del día
- Widget de próximo evento
- Widget de chat rápido

## 🐛 **BUGS Y MEJORAS MENORES**

### Para Arreglar
- Eventos all-day no se manejan bien
- Timezone issues
- Conflictos de eventos superpuestos

### Optimizaciones
- Cache de mensajes AI
- Batch processing de notificaciones
- Background sync más eficiente

## 📈 **MONETIZACIÓN** (si fuera comercial)

### Chapelotas Pro
- Múltiples perfiles
- Estadísticas avanzadas
- Integraciones premium
- Sin límite de eventos

### Chapelotas Teams
- Secretaria compartida
- Delegación de tareas
- Reportes de equipo