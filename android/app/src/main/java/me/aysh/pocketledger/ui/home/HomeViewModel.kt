package me.aysh.pocketledger.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.aysh.pocketledger.data.model.Expense
import me.aysh.pocketledger.data.model.MonthSummary
import me.aysh.pocketledger.data.model.Profile
import me.aysh.pocketledger.data.remote.AppException
import me.aysh.pocketledger.data.repository.LedgerRepository
import java.time.YearMonth

data class HomeContent(
    val profile: Profile,
    val summary: MonthSummary,
    val recent: List<Expense>,
)

data class HomeUiState(
    val month: YearMonth = YearMonth.now(),
    val content: HomeContent? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

class HomeViewModel(private val ledger: LedgerRepository) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { ledger.revision.collect { load() } }
    }

    fun refresh() = viewModelScope.launch { load(userInitiated = true) }

    private suspend fun load(userInitiated: Boolean = false) {
        val month = YearMonth.now()
        _state.update {
            it.copy(
                month = month,
                isLoading = it.content == null,
                isRefreshing = userInitiated,
                error = null,
            )
        }
        try {
            val content = coroutineScope {
                val profile = async { ledger.profile() }
                val summary = async { ledger.summary(month) }
                val recent = async { ledger.expenses(month = month).take(RECENT_COUNT) }
                HomeContent(profile.await(), summary.await(), recent.await())
            }
            _state.update { it.copy(content = content, isLoading = false, isRefreshing = false) }
        } catch (e: AppException) {
            _state.update { it.copy(error = e.message, isLoading = false, isRefreshing = false) }
        }
    }

    private companion object {
        const val RECENT_COUNT = 5
    }
}
