package me.aysh.pocketledger.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.aysh.pocketledger.data.model.Category
import me.aysh.pocketledger.data.model.ExpenseDraft
import me.aysh.pocketledger.data.remote.AppException
import me.aysh.pocketledger.data.repository.LedgerRepository
import me.aysh.pocketledger.util.Money
import me.aysh.pocketledger.util.Validation
import java.time.LocalDate

data class ExpenseEditorUiState(
    val expenseId: String? = null,
    val amount: String = "",
    val category: Category = Category.Food,
    val note: String = "",
    val date: LocalDate = LocalDate.now(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val showAmountError: Boolean = false,
    /** Editing an expense that couldn't be loaded: saving would overwrite it with defaults. */
    val loadFailed: Boolean = false,
    val error: String? = null,
    /** True once saved or deleted: the screen should close. */
    val isDone: Boolean = false,
) {
    val isEditing: Boolean get() = expenseId != null
    val amountError: String? get() = if (showAmountError) Validation.amount(amount) else null
    val isBusy: Boolean get() = isLoading || isSaving || isDeleting || loadFailed
}

class ExpenseEditorViewModel(
    private val expenseId: String?,
    private val ledger: LedgerRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ExpenseEditorUiState(expenseId = expenseId, isLoading = expenseId != null))
    val state: StateFlow<ExpenseEditorUiState> = _state.asStateFlow()

    init {
        if (expenseId != null) load(expenseId)
    }

    private fun load(id: String) {
        viewModelScope.launch {
            try {
                val expense = ledger.expense(id)
                _state.update {
                    it.copy(
                        amount = Money.toInput(expense.amountPaise),
                        category = expense.category,
                        note = expense.note,
                        date = expense.spentOn,
                        isLoading = false,
                    )
                }
            } catch (e: AppException) {
                _state.update { it.copy(isLoading = false, loadFailed = true, error = e.message) }
            }
        }
    }

    fun onAmountChange(value: String) = _state.update { it.copy(amount = Money.sanitizeInput(value), error = null) }

    fun onCategoryChange(category: Category) = _state.update { it.copy(category = category) }

    fun onNoteChange(value: String) = _state.update { it.copy(note = value.take(Validation.MAX_NOTE)) }

    /** Future dates aren't allowed: you can only log money you've already spent. */
    fun onDateChange(date: LocalDate) = _state.update { it.copy(date = minOf(date, LocalDate.now())) }

    fun dismissError() = _state.update { it.copy(error = null) }

    fun retryLoad() {
        val id = expenseId ?: return
        _state.update { it.copy(isLoading = true, loadFailed = false, error = null) }
        load(id)
    }

    fun save() {
        val current = _state.value
        if (current.isBusy) return
        val paise = Money.parseToPaise(current.amount)
        if (paise == null) {
            _state.update { it.copy(showAmountError = true) }
            return
        }
        val draft = ExpenseDraft(paise, current.category, current.note.trim(), current.date)
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                if (expenseId == null) ledger.addExpense(draft) else ledger.updateExpense(expenseId, draft)
                _state.update { it.copy(isSaving = false, isDone = true) }
            } catch (e: AppException) {
                _state.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun delete() {
        val id = expenseId ?: return
        if (_state.value.isBusy) return
        _state.update { it.copy(isDeleting = true, error = null) }
        viewModelScope.launch {
            try {
                ledger.deleteExpense(id)
                _state.update { it.copy(isDeleting = false, isDone = true) }
            } catch (e: AppException) {
                _state.update { it.copy(isDeleting = false, error = e.message) }
            }
        }
    }
}
