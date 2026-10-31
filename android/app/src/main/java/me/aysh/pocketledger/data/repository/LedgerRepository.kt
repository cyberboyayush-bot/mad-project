package me.aysh.pocketledger.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import me.aysh.pocketledger.data.model.Category
import me.aysh.pocketledger.data.model.Expense
import me.aysh.pocketledger.data.model.ExpenseDraft
import me.aysh.pocketledger.data.model.MonthSummary
import me.aysh.pocketledger.data.model.Profile
import me.aysh.pocketledger.data.remote.PocketLedgerApi
import me.aysh.pocketledger.data.remote.UpdateProfileRequest
import me.aysh.pocketledger.data.remote.apiCall
import me.aysh.pocketledger.data.remote.requireSuccess
import me.aysh.pocketledger.data.remote.toModel
import me.aysh.pocketledger.data.remote.toRequest
import java.time.YearMonth

/**
 * Single entry point for expense and profile data.
 * [revision] increments after every successful write so screens know to reload.
 */
class LedgerRepository(
    private val api: PocketLedgerApi,
    private val json: Json,
) {

    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    private fun bump() = _revision.update { it + 1 }

    suspend fun expenses(month: YearMonth? = null, category: Category? = null, query: String? = null): List<Expense> =
        apiCall(json) {
            api.expenses(
                month = month?.toString(),
                category = category?.key,
                query = query?.trim()?.takeIf { it.isNotEmpty() },
            ).expenses.map { it.toModel() }
        }

    suspend fun expense(id: String): Expense = apiCall(json) { api.expense(id).expense.toModel() }

    suspend fun addExpense(draft: ExpenseDraft): Expense =
        apiCall(json) { api.createExpense(draft.toRequest()).expense.toModel() }.also { bump() }

    suspend fun updateExpense(id: String, draft: ExpenseDraft): Expense =
        apiCall(json) { api.updateExpense(id, draft.toRequest()).expense.toModel() }.also { bump() }

    suspend fun deleteExpense(id: String) {
        apiCall(json) { api.deleteExpense(id).requireSuccess(json) }
        bump()
    }

    suspend fun summary(month: YearMonth): MonthSummary = apiCall(json) { api.summary(month.toString()).toModel() }

    suspend fun profile(): Profile = apiCall(json) { api.profile().profile.toModel() }

    suspend fun updateProfile(name: String? = null, monthlyBudgetPaise: Long? = null): Profile =
        apiCall(json) {
            api.updateProfile(UpdateProfileRequest(name = name?.trim(), monthlyBudget = monthlyBudgetPaise)).profile.toModel()
        }.also { bump() }
}
