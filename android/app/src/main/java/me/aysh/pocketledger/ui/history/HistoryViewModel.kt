package me.aysh.pocketledger.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.aysh.pocketledger.data.model.Category
import me.aysh.pocketledger.data.model.Expense
import me.aysh.pocketledger.data.model.ExpenseDraft
import me.aysh.pocketledger.data.remote.AppException
import me.aysh.pocketledger.data.repository.LedgerRepository
import java.time.LocalDate
import java.time.YearMonth

data class DayGroup(val date: LocalDate, val totalPaise: Long, val expenses: List<Expense>)

data class HistoryUiState(
    val month: YearMonth = YearMonth.now(),
    val query: String = "",
    val category: Category? = null,
    val groups: List<DayGroup> = emptyList(),
    val monthTotalPaise: Long = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    val canGoForward: Boolean get() = month < YearMonth.now()
    val isFiltered: Boolean get() = query.isNotBlank() || category != null
    val isEmpty: Boolean get() = !isLoading && error == null && groups.isEmpty()
}

sealed interface HistoryEvent {
    data class Deleted(val expense: Expense) : HistoryEvent
    data class Failed(val message: String) : HistoryEvent
}

private data class Filters(val month: YearMonth, val query: String, val category: Category?)

@OptIn(FlowPreview::class)
class HistoryViewModel(private val ledger: LedgerRepository) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    private val _events = Channel<HistoryEvent>(Channel.BUFFERED)
    val events: Flow<HistoryEvent> = _events.receiveAsFlow()

    private val retryTrigger = MutableStateFlow(0)

    init {
        val filters = _state
            .map { Filters(it.month, it.query.trim(), it.category) }
            .distinctUntilChanged()
            .debounce { if (it.query.isEmpty()) 0L else SEARCH_DEBOUNCE_MS }

        viewModelScope.launch {
            combine(filters, ledger.revision, retryTrigger) { f, _, _ -> f }.collectLatest(::load)
        }
    }

    fun previousMonth() = _state.update { it.copy(month = it.month.minusMonths(1), isLoading = true) }

    fun nextMonth() = _state.update {
        if (it.canGoForward) it.copy(month = it.month.plusMonths(1), isLoading = true) else it
    }

    fun onQueryChange(value: String) = _state.update { it.copy(query = value.take(60)) }

    fun onCategoryToggle(category: Category) =
        _state.update { it.copy(category = if (it.category == category) null else category) }

    fun clearFilters() = _state.update { it.copy(query = "", category = null) }

    fun retry() = retryTrigger.update { it + 1 }

    fun delete(expense: Expense) {
        // Hide immediately for a snappy swipe; the reload after the server call confirms it.
        _state.update { state -> state.copy(groups = state.groups.without(expense.id)) }
        viewModelScope.launch {
            try {
                ledger.deleteExpense(expense.id)
                _events.send(HistoryEvent.Deleted(expense))
            } catch (e: AppException) {
                retry()
                _events.send(HistoryEvent.Failed(e.message))
            }
        }
    }

    fun undoDelete(expense: Expense) {
        viewModelScope.launch {
            try {
                ledger.addExpense(ExpenseDraft(expense.amountPaise, expense.category, expense.note, expense.spentOn))
            } catch (e: AppException) {
                _events.send(HistoryEvent.Failed(e.message))
            }
        }
    }

    private suspend fun load(filters: Filters) {
        _state.update { it.copy(isLoading = true, error = null) }
        try {
            val expenses = ledger.expenses(
                month = filters.month,
                category = filters.category,
                query = filters.query,
            )
            val groups = expenses
                .groupBy { it.spentOn }
                .map { (date, items) -> DayGroup(date, items.sumOf { it.amountPaise }, items) }
                .sortedByDescending { it.date }
            _state.update {
                it.copy(groups = groups, monthTotalPaise = groups.sumOf { g -> g.totalPaise }, isLoading = false)
            }
        } catch (e: AppException) {
            _state.update { it.copy(error = e.message, isLoading = false) }
        }
    }

    private fun List<DayGroup>.without(id: String): List<DayGroup> = mapNotNull { group ->
        val remaining = group.expenses.filterNot { it.id == id }
        if (remaining.isEmpty()) null else group.copy(expenses = remaining, totalPaise = remaining.sumOf { it.amountPaise })
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
