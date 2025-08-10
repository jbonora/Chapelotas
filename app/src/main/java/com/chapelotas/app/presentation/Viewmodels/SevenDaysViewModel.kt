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

data class TimeSlot(
    val start: LocalTime,
    val end: LocalTime,
    val description: String,
    val durationMinutes: Long
)

enum class BlockStatus {
    HAS_SLOTS,
    FULLY_BOOKED,
    OUTSIDE_WORK_HOURS,
    IN_THE_PAST
}

data class TimeBlock(
    val status: BlockStatus,
    val slots: List<TimeSlot> = emptyList()
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

            val allFreeSlotsForDay = getFreeTimeSlotsForDay(workStart, workEnd, tasksForDay)

            scheduleByDay[currentDate] = DailySchedule(
                morning = getTimeBlockForPeriod(currentDate, workStart, lunchStart, allFreeSlotsForDay, tasksForDay),
                lunch = getTimeBlockForPeriod(currentDate, lunchStart, lunchEnd, allFreeSlotsForDay, tasksForDay),
                afternoon = getTimeBlockForPeriod(currentDate, lunchEnd, dinnerStart, allFreeSlotsForDay, tasksForDay),
                dinner = getTimeBlockForPeriod(currentDate, dinnerStart, dinnerEnd, allFreeSlotsForDay, tasksForDay),
                night = getTimeBlockForPeriod(currentDate, dinnerEnd, workEnd, allFreeSlotsForDay, tasksForDay)
            )
        }
        return scheduleByDay
    }

    private fun getFreeTimeSlotsForDay(
        workStart: LocalTime,
        workEnd: LocalTime,
        tasks: List<Task>
    ): List<TimeSlot> {
        if (!workStart.isBefore(workEnd)) return emptyList()

        val busyPeriods = tasks.map {
            val taskEnd = it.endTime?.toLocalTime() ?: it.scheduledTime.toLocalTime().plusHours(1)
            val taskStartWithTravel = it.scheduledTime.toLocalTime().minusMinutes(it.travelTimeMinutes.toLong())
            val taskEndWithTravel = taskEnd.plusMinutes(it.travelTimeMinutes.toLong())
            Pair(taskStartWithTravel, taskEndWithTravel)
        }.sortedBy { it.first }

        val mergedBusyPeriods = mutableListOf<Pair<LocalTime, LocalTime>>()
        for (period in busyPeriods) {
            if (mergedBusyPeriods.isEmpty() || period.first.isAfter(mergedBusyPeriods.last().second)) {
                mergedBusyPeriods.add(period)
            } else {
                val last = mergedBusyPeriods.last()
                mergedBusyPeriods[mergedBusyPeriods.lastIndex] = Pair(last.first, maxOf(last.second, period.second))
            }
        }

        val freePeriods = mutableListOf<TimeSlot>()
        var lastEndTime = workStart

        for (busyPeriod in mergedBusyPeriods) {
            if (busyPeriod.first.isAfter(lastEndTime)) {
                freePeriods.add(TimeSlot(lastEndTime, busyPeriod.first, "", 0))
            }
            lastEndTime = maxOf(lastEndTime, busyPeriod.second)
        }

        if (lastEndTime.isBefore(workEnd)) {
            freePeriods.add(TimeSlot(lastEndTime, workEnd, "", 0))
        }

        return freePeriods.flatMap { smartSplitSlot(it, 60, 30) }
    }

    private fun getTimeBlockForPeriod(
        currentDate: LocalDate,
        periodStart: LocalTime,
        periodEnd: LocalTime,
        allFreeSlotsForDay: List<TimeSlot>,
        dailyTasks: List<Task>
    ): TimeBlock {
        val now = LocalTime.now()

        if (currentDate.isEqual(today) && periodEnd.isBefore(now)) {
            return TimeBlock(status = BlockStatus.IN_THE_PAST)
        }
        if(!periodStart.isBefore(periodEnd)) return TimeBlock(status = BlockStatus.OUTSIDE_WORK_HOURS)

        var slotsInPeriod = allFreeSlotsForDay.filter {
            !it.start.isBefore(periodStart) && !it.end.isAfter(periodEnd)
        }

        if (currentDate.isEqual(today)) {
            slotsInPeriod = slotsInPeriod.filter { it.end.isAfter(now) }
        }

        return if (slotsInPeriod.isNotEmpty()) {
            TimeBlock(status = BlockStatus.HAS_SLOTS, slots = slotsInPeriod)
        } else {
            val isBookedByTask = dailyTasks.any {
                val taskStartWithTravel = it.scheduledTime.toLocalTime().minusMinutes(it.travelTimeMinutes.toLong())
                val taskEndWithTravel = (it.endTime?.toLocalTime() ?: it.scheduledTime.toLocalTime().plusHours(1)).plusMinutes(it.travelTimeMinutes.toLong())
                taskStartWithTravel.isBefore(periodEnd) && taskEndWithTravel.isAfter(periodStart)
            }
            if (isBookedByTask) {
                TimeBlock(status = BlockStatus.FULLY_BOOKED)
            } else {
                TimeBlock(status = BlockStatus.HAS_SLOTS, slots = emptyList())
            }
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

        return chunks
    }
}

private fun maxOf(a: LocalTime, b: LocalTime): LocalTime = if (a.isAfter(b)) a else b