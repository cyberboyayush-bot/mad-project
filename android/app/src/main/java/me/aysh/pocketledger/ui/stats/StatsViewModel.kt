package me.aysh.pocketledger.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.aysh.pocketledger.data.model.MonthSummary
import me.aysh.pocketledger.data.remote.AppException
import me.aysh.pocketledger.data.repository.LedgerRepository
import java.time.LocalDate
import java.time.YearMonth

data class StatsUiState(
    val month: YearMonth = YearMonth.now(),
    val summary: MonthSummary? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    val canGoForward: Boolean get() = month < YearMonth.now()

    /** One bar per day of the month, zero-filled, ready for a chart. */
    val dailyTotals: List<Long>
        get() {
            val s = summary ?: return emptyList()
            val byDate = s.byDay.associate { it.date to it.totalPaise }
            return (1..s.month.lengthOfMonth()).map { day -> byDate[s.month.atDay(day)] ?: 0L }
        }

    /** Average spend per elapsed day (whole month for past months). */
    val dailyAveragePaise: Long
        get() {
            val s = summary ?: return 0
            val today = LocalDate.now()
            val days = if (s.month == YearMonth.from(today)) today.dayOfMonth else s.month.lengthOfMonth()
            return s.totalPaise / days.coerceAtLeast(1)
        }

    /** Percentage change vs the previous month, or null when there's nothing to compare. */
    val changeVsPrevious: Int?
        get() {
            val s = summary ?: return null
            if (s.previousTotalPaise == 0L) return null
            return (((s.totalPaise - s.previousTotalPaise) * 100) / s.previousTotalPaise).toInt()
        }
}

class StatsViewModel(private val ledger: LedgerRepository) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())
    private val retryTrigger = MutableStateFlow(0)

    private val _state = MutableStateFlow(StatsUiState())
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(month, ledger.revision, retryTrigger) { m, _, _ -> m }.collectLatest { m ->
                _state.update { it.copy(month = m, isLoading = true, error = null) }
                try {
                    val summary = ledger.summary(m)
                    _state.update { it.copy(summary = summary, isLoading = false) }
                } catch (e: AppException) {
                    _state.update { it.copy(error = e.message, isLoading = false, summary = null) }
                }
            }
        }
    }

    fun previousMonth() = month.update { it.minusMonths(1) }

    fun nextMonth() = month.update { if (it < YearMonth.now()) it.plusMonths(1) else it }

    fun retry() = retryTrigger.update { it + 1 }
}
