package com.chapelotas.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chapelotas.app.domain.models.AppSettings
import com.chapelotas.app.domain.models.Task
import com.chapelotas.app.domain.repositories.PreferencesRepository
import com.chapelotas.app.domain.repositories.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

// --- NUEVOS DATA CLASSES PARA REPRESENTAR EL HORARIO ---

// Representa un hueco libre y clickeable
data class TimeSlot(
    val start: LocalTime,
    val end: LocalTime,
    val description: String,
    val durationMinutes: Long
)

// Sealed interface para los items del horario
sealed class ScheduleItem {
    data class Free(val slot: TimeSlot) : ScheduleItem()
    data class Busy(
        val taskTitle: String,
        val start: LocalTime,
        val end: LocalTime,
        val description: String // e.g., "Viaje", "Reunión"
    ) : ScheduleItem()
}


enum class BlockStatus {
    HAS_SLOTS,
    FULLY_BOOKED,
    OUTSIDE_WORK_HOURS,
    IN_THE_PAST
}

// TimeBlock ahora contiene una lista de ScheduleItem
data class TimeBlock(
    val status: BlockStatus,
    val items: List<ScheduleItem> = emptyList()
)

data class DailySchedule(
    val morning: TimeBlock,
    val lunch: TimeBlock,
    val afternoon: TimeBlock,
    val dinner: TimeBlock,
    val night: TimeBlock
)

data class SevenDaysUiState(
    val tasksByDay: Map<LocalDate, List<Task>> = emptyMap(),
    val weekDays: List<LocalDate> = emptyList(),
    val scheduleByDay: Map<LocalDate, DailySchedule> = emptyMap()
)

@HiltViewModel
class SevenDaysViewModel @Inject constructor(
    taskRepository: TaskRepository,
    preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val today = LocalDate.now()
    private val startDate = today
    private val endDate = today.plusDays(6)

    private val tasksFlow = taskRepository.observeTasksForDateRange(startDate, endDate)
    private val settingsFlow = preferencesRepository.observeAppSettings()

    val uiState: StateFlow<SevenDaysUiState> =
        combine(tasksFlow, settingsFlow) { tasks, settings ->
            val schedule = calculateDailySchedules(tasks, settings)
            val groupedTasks = tasks.groupBy { it.scheduledTime.toLocalDate() }
            val nextSevenDays = (0..6).map { startDate.plusDays(it.toLong()) }

            SevenDaysUiState(
                tasksByDay = groupedTasks,
                weekDays = nextSevenDays,
                scheduleByDay = schedule
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SevenDaysUiState()
        )

    private fun getLocalTime(timeString: String, defaultTime: LocalTime): LocalTime {
        return try { LocalTime.parse(timeString) } catch (e: Exception) { defaultTime }
    }

    private fun calculateDailySchedules(tasks: List<Task>, settings: AppSettings): Map<LocalDate, DailySchedule> {
        val scheduleByDay = mutableMapOf<LocalDate, DailySchedule>()

        val workStart = getLocalTime(settings.workStartTime, LocalTime.of(8, 0))
        val workEnd = getLocalTime(settings.workEndTime, LocalTime.of(23, 0))
        val lunchStart = getLocalTime(settings.lunchStartTime, LocalTime.of(13, 0))
        val lunchEnd = getLocalTime(settings.lunchEndTime, LocalTime.of(14, 0))
        val dinnerStart = getLocalTime(settings.dinnerStartTime, LocalTime.of(20, 0))
        val dinnerEnd = getLocalTime(settings.dinnerEndTime, LocalTime.of(21, 0))

        for (i in 0..6) {
            val currentDate = today.plusDays(i.toLong())
            val tasksForDay = tasks.filter { it.scheduledTime.toLocalDate() == currentDate }

            val allScheduleItems = buildCombinedScheduleForDay(workStart, workEnd, tasksForDay)

            scheduleByDay[currentDate] = DailySchedule(
                morning = getTimeBlockForPeriod(currentDate, workStart, lunchStart, allScheduleItems),
                lunch = getTimeBlockForPeriod(currentDate, lunchStart, lunchEnd, allScheduleItems),
                afternoon = getTimeBlockForPeriod(currentDate, lunchEnd, dinnerStart, allScheduleItems),
                dinner = getTimeBlockForPeriod(currentDate, dinnerStart, dinnerEnd, allScheduleItems),
                night = getTimeBlockForPeriod(currentDate, dinnerEnd, workEnd, allScheduleItems)
            )
        }
        return scheduleByDay
    }

    private fun buildCombinedScheduleForDay(
        workStart: LocalTime,
        workEnd: LocalTime,
        tasks: List<Task>
    ): List<ScheduleItem> {
        if (!workStart.isBefore(workEnd)) return emptyList()

        // 1. Crear una lista de todos los bloques ocupados (viajes y eventos)
        val busyBlocks = tasks.flatMap { task ->
            val taskStart = task.scheduledTime.toLocalTime()
            val taskEnd = task.endTime?.toLocalTime() ?: taskStart.plusHours(1)
            val travel = task.travelTimeMinutes.toLong()
            val travelStartTime = taskStart.minusMinutes(travel)
            val travelEndTime = taskEnd.plusMinutes(travel)

            listOfNotNull(
                // Viaje de ida
                if (travel > 0) ScheduleItem.Busy(task.title, travelStartTime, taskStart, "Viaje ida") else null,
                // Evento
                ScheduleItem.Busy(task.title, taskStart, taskEnd, task.title.split(" ").take(3).joinToString(" ")),
                // Viaje de vuelta
                if (travel > 0) ScheduleItem.Busy(task.title, taskEnd, travelEndTime, "Viaje vuelta") else null
            )
        }.sortedBy { it.start }

        // 2. Fusionar bloques ocupados que se solapan
        val mergedBusyBlocks = mutableListOf<ScheduleItem.Busy>()
        for (block in busyBlocks) {
            if (mergedBusyBlocks.isEmpty() || block.start >= mergedBusyBlocks.last().end) {
                mergedBusyBlocks.add(block)
            } else {
                // --- CORRECCIÓN AQUÍ ---
                // Se reemplaza removeLast() por una alternativa más segura
                val last = mergedBusyBlocks.removeAt(mergedBusyBlocks.lastIndex)
                val newEnd = if (block.end.isAfter(last.end)) block.end else last.end
                // Si se fusionan, priorizamos mostrar el nombre del evento principal
                val newDescription = if (last.description.startsWith("Viaje") && !block.description.startsWith("Viaje")) {
                    block.description
                } else {
                    last.description
                }
                mergedBusyBlocks.add(last.copy(end = newEnd, description = newDescription))
            }
        }

        // 3. Construir la lista final combinando bloques libres y ocupados
        val combinedSchedule = mutableListOf<ScheduleItem>()
        var cursor = workStart

        for (busyBlock in mergedBusyBlocks) {
            if (busyBlock.start > cursor) {
                // Hay un hueco libre antes de este bloque ocupado
                val freeSlot = TimeSlot(cursor, busyBlock.start, "", 0)
                combinedSchedule.addAll(smartSplitSlot(freeSlot, 60, 30).map { ScheduleItem.Free(it) })
            }
            combinedSchedule.add(busyBlock)
            cursor = busyBlock.end
        }

        // 4. Añadir el último hueco libre si lo hay
        if (cursor < workEnd) {
            val finalFreeSlot = TimeSlot(cursor, workEnd, "", 0)
            combinedSchedule.addAll(smartSplitSlot(finalFreeSlot, 60, 30).map { ScheduleItem.Free(it) })
        }

        return combinedSchedule
    }

    private fun getTimeBlockForPeriod(
        currentDate: LocalDate,
        periodStart: LocalTime,
        periodEnd: LocalTime,
        allScheduleItems: List<ScheduleItem>
    ): TimeBlock {
        val now = LocalTime.now()

        if (currentDate.isEqual(today) && periodEnd.isBefore(now)) {
            return TimeBlock(status = BlockStatus.IN_THE_PAST)
        }
        if(!periodStart.isBefore(periodEnd)) return TimeBlock(status = BlockStatus.OUTSIDE_WORK_HOURS)

        var itemsInPeriod = allScheduleItems.filter { item ->
            val itemStart = when(item) {
                is ScheduleItem.Busy -> item.start
                is ScheduleItem.Free -> item.slot.start
            }
            val itemEnd = when(item) {
                is ScheduleItem.Busy -> item.end
                is ScheduleItem.Free -> item.slot.end
            }
            itemStart < periodEnd && itemEnd > periodStart
        }

        if (currentDate.isEqual(today)) {
            itemsInPeriod = itemsInPeriod.filter {
                val itemEnd = when(it) {
                    is ScheduleItem.Busy -> it.end
                    is ScheduleItem.Free -> it.slot.end
                }
                itemEnd.isAfter(now)
            }
        }

        val hasFreeSlots = itemsInPeriod.any { it is ScheduleItem.Free }

        return if (itemsInPeriod.isNotEmpty()) {
            TimeBlock(
                status = if (hasFreeSlots) BlockStatus.HAS_SLOTS else BlockStatus.FULLY_BOOKED,
                items = itemsInPeriod
            )
        } else {
            TimeBlock(status = BlockStatus.FULLY_BOOKED) // Si no hay items, está ocupado o es un periodo inválido
        }
    }

    private fun smartSplitSlot(slot: TimeSlot, chunkDurationMinutes: Long, minFragmentDurationMinutes: Long): List<TimeSlot> {
        val chunks = mutableListOf<TimeSlot>()
        var currentStartTime = slot.start

        // Redondear al próximo cuarto de hora si es necesario
        if (currentStartTime.minute % 15 != 0) {
            currentStartTime = currentStartTime.plusMinutes(15 - (currentStartTime.minute % 15).toLong())
        }

        // Rellenar hasta la próxima hora en punto
        val nextFullHour = currentStartTime.truncatedTo(java.time.temporal.ChronoUnit.HOURS).plusHours(1)
        if ((nextFullHour.isBefore(slot.end) || nextFullHour == slot.end) && currentStartTime.isBefore(nextFullHour) ) {
            val fragmentDuration = Duration.between(currentStartTime, nextFullHour).toMinutes()
            if (fragmentDuration >= minFragmentDurationMinutes) {
                val description = "Bloque de $fragmentDuration min"
                chunks.add(TimeSlot(currentStartTime, nextFullHour, description, fragmentDuration))
            }
            currentStartTime = nextFullHour
        }

        // Crear bloques grandes
        while (Duration.between(currentStartTime, slot.end).toMinutes() >= chunkDurationMinutes) {
            val nextEndTime = currentStartTime.plusMinutes(chunkDurationMinutes)
            if (!nextEndTime.isAfter(slot.end)) {
                val description = "Bloque de $chunkDurationMinutes min"
                chunks.add(TimeSlot(currentStartTime, nextEndTime, description, chunkDurationMinutes))
            }
            currentStartTime = nextEndTime
        }

        // Rellenar el fragmento restante
        val remainingMinutes = Duration.between(currentStartTime, slot.end).toMinutes()
        if (remainingMinutes >= minFragmentDurationMinutes) {
            val description = "Bloque de $remainingMinutes min"
            chunks.add(TimeSlot(currentStartTime, slot.end, description, remainingMinutes))
        }

        return chunks
    }
}
