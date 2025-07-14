# Archivos modificados hoy

## `/app/src/main/java/com/chapelotas/app/data/database/`

- **ChapelotasDatabase.kt** - Versión 1 sin migraciones, 8 entidades
- **converters/Converters.kt** - (si lo modificaste)

## `/app/src/main/java/com/chapelotas/app/data/database/entities/`

- **ConversationLog.kt** - Agregados threadId y messageStatus
- **MonkeyAgenda.kt** - NUEVO
- **ChatThread.kt** - NUEVO
- **Enums.kt** - Agregado CANCELLED y SARCASTIC_NUDGE

## `/app/src/main/java/com/chapelotas/app/data/database/daos/`

- **ConversationLogDao.kt** - Métodos para threads
- **MonkeyAgendaDao.kt** - NUEVO
- **ChatThreadDao.kt** - NUEVO

## `/app/src/main/java/com/chapelotas/app/data/`

- **DataModule.kt** - Providers para nuevos DAOs y servicios

## `/app/src/main/java/com/chapelotas/app/data/preferences/`

- **PreferencesRepositoryImpl.kt** - NUEVO/CORREGIDO

## `/app/src/main/java/com/chapelotas/app/data/receivers/`

- **BootReceiver.kt** - NUEVO

## `/app/src/main/java/com/chapelotas/app/domain/usecases/`

- **MonkeyAgendaService.kt** - NUEVO
- **UnifiedMonkeyService.kt** - Simplificado a 250 líneas
- **MigrateToMonkeyAgendaUseCase.kt** - NUEVO
- **InitializeChatThreadsUseCase.kt** - NUEVO
- **CalendarSyncUseCase.kt** - NUEVO

## `/app/src/main/java/com/chapelotas/app/domain/repositories/`

- **PreferencesRepository.kt** - Métodos de tracking agregados

## `/app/src/main/java/com/chapelotas/app/domain/entities/`

- **UserPreferences.kt** - (confirmado existente)
- **CalendarEvent.kt** - (confirmado existente)

## `/app/src/main/java/com/chapelotas/app/domain/events/`

- **ChapelotasEvent.kt** - Agregado CalendarSyncCompleted

## `/app/src/main/java/com/chapelotas/app/presentation/viewmodels/`

- **CalendarMonitorViewModel.kt** - iniciarDia() mejorado, refreshEventStates()

## `/app/src/main/java/com/chapelotas/app/di/`

- **Qualifiers.kt** - NUEVO (ApplicationScope)

## `/app/src/main/`

- **AndroidManifest.xml** - Agregar BootReceiver y RECEIVE_BOOT_COMPLETED

## `/app/`

- **build.gradle** - Dependencia datastore-preferences:1.0.0