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

// --- DATA CLASSES PARA REPRESENTAR EL HORARIO ---

data class TimeSlot(
    val start: LocalTime,
    val end: LocalTime,
    val description: String,
    val durationMinutes: Long
)

sealed class ScheduleItem {
    data class Free(val slot: TimeSlot) : ScheduleItem()
    data class Busy(
        val taskTitle: String,
        val start: LocalTime,
        val end: LocalTime,
        val description: String,
        val calendarEventId: Long?,
        val isFromCalendar: Boolean
    ) : ScheduleItem()
}

enum class BlockStatus {
    HAS_SLOTS,
    FULLY_BOOKED,
    OUTSIDE_WORK_HOURS,
    IN_THE_PAST
}

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

// --- NUEVO DATA CLASS PARA LA PANTALLA ---
data class DayInfo(
    val date: LocalDate,
    val timedTasks: List<Task>,
    val allDayTasks: List<Task>,
    val schedule: DailySchedule
)

data class SevenDaysUiState(
    val daysInfo: List<DayInfo> = emptyList(),
    val isLoading: Boolean = true
)
// --- FIN DE NUEVOS DATA CLASSES ---


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
            val daysInfoList = mutableListOf<DayInfo>()
            val weekDays = (0..6).map { startDate.plusDays(it.toLong()) }

            for (day in weekDays) {
                // Separamos los eventos de todo el día de los que tienen hora
                val allTasksForDay = tasks.filter { it.scheduledTime.toLocalDate() == day }
                val (allDayTasks, timedTasks) = allTasksForDay.partition { it.isAllDay }

                val schedule = calculateDailyScheduleForDay(day, timedTasks, settings)

                daysInfoList.add(
                    DayInfo(
                        date = day,
                        timedTasks = timedTasks.sortedBy { it.scheduledTime },
                        allDayTasks = allDayTasks,
                        schedule = schedule
                    )
                )
            }

            SevenDaysUiState(daysInfo = daysInfoList, isLoading = false)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SevenDaysUiState(isLoading = true)
        )

    private fun getLocalTime(timeString: String, defaultTime: LocalTime): LocalTime {
        return try { LocalTime.parse(timeString) } catch (e: Exception) { defaultTime }
    }

    // --- LÓGICA REFACTORIZADA ---
    private fun calculateDailyScheduleForDay(currentDate: LocalDate, tasksForDay: List<Task>, settings: AppSettings): DailySchedule {
        val workStart = getLocalTime(settings.workStartTime, LocalTime.of(8, 0))
        val workEnd = getLocalTime(settings.workEndTime, LocalTime.of(23, 0))
        val lunchStart = getLocalTime(settings.lunchStartTime, LocalTime.of(13, 0))
        val lunchEnd = getLocalTime(settings.lunchEndTime, LocalTime.of(14, 0))
        val dinnerStart = getLocalTime(settings.dinnerStartTime, LocalTime.of(20, 0))
        val dinnerEnd = getLocalTime(settings.dinnerEndTime, LocalTime.of(21, 0))

        // Los eventos de todo el día ya no se usan para calcular los huecos
        val allScheduleItems = buildCombinedScheduleForDay(workStart, workEnd, tasksForDay)

        return DailySchedule(
            morning = getTimeBlockForPeriod(currentDate, workStart, lunchStart, allScheduleItems),
            lunch = getTimeBlockForPeriod(currentDate, lunchStart, lunchEnd, allScheduleItems),
            afternoon = getTimeBlockForPeriod(currentDate, lunchEnd, dinnerStart, allScheduleItems),
            dinner = getTimeBlockForPeriod(currentDate, dinnerStart, dinnerEnd, allScheduleItems),
            night = getTimeBlockForPeriod(currentDate, dinnerEnd, workEnd, allScheduleItems)
        )
    }

    private fun buildCombinedScheduleForDay(
        workStart: LocalTime,
        workEnd: LocalTime,
        tasks: List<Task>
    ): List<ScheduleItem> {
        if (!workStart.isBefore(workEnd)) return emptyList()

        val busyBlocks = tasks.flatMap { task ->
            val taskStart = task.scheduledTime.toLocalTime()
            val taskEnd = task.endTime?.toLocalTime() ?: taskStart.plusHours(1)
            val travel = task.travelTimeMinutes.toLong()
            val travelStartTime = taskStart.minusMinutes(travel)
            val travelEndTime = taskEnd.plusMinutes(travel)

            listOfNotNull(
                if (travel > 0) ScheduleItem.Busy(task.title, travelStartTime, taskStart, "Viaje ida", task.calendarEventId, task.isFromCalendar) else null,
                ScheduleItem.Busy(task.title, taskStart, taskEnd, task.title.split(" ").take(3).joinToString(" "), task.calendarEventId, task.isFromCalendar),
                if (travel > 0) ScheduleItem.Busy(task.title, taskEnd, travelEndTime, "Viaje vuelta", task.calendarEventId, task.isFromCalendar) else null
            )
        }.sortedBy { it.start }

        val mergedBusyBlocks = mutableListOf<ScheduleItem.Busy>()
        for (block in busyBlocks) {
            if (mergedBusyBlocks.isEmpty() || block.start >= mergedBusyBlocks.last().end) {
                mergedBusyBlocks.add(block)
            } else {
                val last = mergedBusyBlocks.removeAt(mergedBusyBlocks.lastIndex)
                val newEnd = if (block.end.isAfter(last.end)) block.end else last.end
                val newDescription = if (last.description.startsWith("Viaje") && !block.description.startsWith("Viaje")) {
                    block.description
                } else {
                    last.description
                }
                mergedBusyBlocks.add(last.copy(end = newEnd, description = newDescription, calendarEventId = block.calendarEventId, isFromCalendar = block.isFromCalendar))
            }
        }

        val combinedSchedule = mutableListOf<ScheduleItem>()
        var cursor = workStart

        for (busyBlock in mergedBusyBlocks) {
            if (busyBlock.start > cursor) {
                val freeSlot = TimeSlot(cursor, busyBlock.start, "", 0)
                combinedSchedule.addAll(smartSplitSlot(freeSlot, 60, 30).map { ScheduleItem.Free(it) })
            }
            combinedSchedule.add(busyBlock)
            cursor = busyBlock.end
        }

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
            TimeBlock(status = BlockStatus.FULLY_BOOKED)
        }
    }

    private fun smartSplitSlot(slot: TimeSlot, chunkDurationMinutes: Long, minFragmentDurationMinutes: Long): List<TimeSlot> {
        val chunks = mutableListOf<TimeSlot>()
        var currentStartTime = slot.start

        if (currentStartTime.minute % 15 != 0) {
            currentStartTime = currentStartTime.plusMinutes(15 - (currentStartTime.minute % 15).toLong())
        }

        val nextFullHour = currentStartTime.truncatedTo(java.time.temporal.ChronoUnit.HOURS).plusHours(1)
        if ((nextFullHour.isBefore(slot.end) || nextFullHour == slot.end) && currentStartTime.isBefore(nextFullHour) ) {
            val fragmentDuration = Duration.between(currentStartTime, nextFullHour).toMinutes()
            if (fragmentDuration >= minFragmentDurationMinutes) {
                val description = "Bloque de $fragmentDuration min"
                chunks.add(TimeSlot(currentStartTime, nextFullHour, description, fragmentDuration))
            }
            currentStartTime = nextFullHour
        }

        while (Duration.between(currentStartTime, slot.end).toMinutes() >= chunkDurationMinutes) {
            val nextEndTime = currentStartTime.plusMinutes(chunkDurationMinutes)
            if (!nextEndTime.isAfter(slot.end)) {
                val description = "Bloque de $chunkDurationMinutes min"
                chunks.add(TimeSlot(currentStartTime, nextEndTime, description, chunkDurationMinutes))
            }
            currentStartTime = nextEndTime
        }

        val remainingMinutes = Duration.between(currentStartTime, slot.end).toMinutes()
        if (remainingMinutes >= minFragmentDurationMinutes) {
            val description = "Bloque de $remainingMinutes min"
            chunks.add(TimeSlot(currentStartTime, slot.end, description, remainingMinutes))
        }

        return chunks
    }
}