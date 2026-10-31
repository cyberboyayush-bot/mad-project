package me.aysh.pocketledger.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.aysh.pocketledger.data.model.Profile
import me.aysh.pocketledger.data.remote.AppException
import me.aysh.pocketledger.data.repository.AuthRepository
import me.aysh.pocketledger.data.repository.LedgerRepository
import me.aysh.pocketledger.util.Money
import me.aysh.pocketledger.util.Validation

/** Which inline editor sheet is open, with its current text. */
sealed interface ProfileEditor {
    data class Name(val value: String) : ProfileEditor {
        val error: String? get() = Validation.name(value)
    }

    data class Budget(val value: String) : ProfileEditor {
        /** Empty input clears the budget. */
        val paise: Long? get() = if (value.isBlank()) 0L else Money.parseToPaise(value)
        val error: String? get() = if (paise == null) "Enter a budget up to ₹10,00,000" else null
    }
}

data class ProfileUiState(
    val profile: Profile? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val editor: ProfileEditor? = null,
    val isSavingEditor: Boolean = false,
    val isSigningOut: Boolean = false,
)

class ProfileViewModel(
    private val ledger: LedgerRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = it.profile == null, error = null) }
        viewModelScope.launch {
            try {
                val profile = ledger.profile()
                _state.update { it.copy(profile = profile, isLoading = false) }
            } catch (e: AppException) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun editName() = _state.update { s -> s.copy(editor = ProfileEditor.Name(s.profile?.name.orEmpty())) }

    fun editBudget() = _state.update { s ->
        val budget = s.profile?.monthlyBudgetPaise ?: 0
        s.copy(editor = ProfileEditor.Budget(if (budget > 0) Money.toInput(budget) else ""))
    }

    fun onEditorChange(value: String) = _state.update { s ->
        when (val editor = s.editor) {
            is ProfileEditor.Name -> s.copy(editor = editor.copy(value = value.take(Validation.MAX_NAME)))
            is ProfileEditor.Budget -> s.copy(editor = editor.copy(value = Money.sanitizeInput(value)))
            null -> s
        }
    }

    fun dismissEditor() = _state.update { if (it.isSavingEditor) it else it.copy(editor = null) }

    fun saveEditor() {
        val editor = _state.value.editor ?: return
        if (_state.value.isSavingEditor) return
        val request: suspend () -> Profile = when (editor) {
            is ProfileEditor.Name -> {
                if (editor.error != null) return
                { ledger.updateProfile(name = editor.value) }
            }
            is ProfileEditor.Budget -> {
                val paise = editor.paise ?: return
                { ledger.updateProfile(monthlyBudgetPaise = paise) }
            }
        }
        _state.update { it.copy(isSavingEditor = true) }
        viewModelScope.launch {
            try {
                val profile = request()
                _state.update { it.copy(profile = profile, editor = null, isSavingEditor = false) }
                _messages.send(if (editor is ProfileEditor.Name) "Name updated" else "Budget updated")
            } catch (e: AppException) {
                _state.update { it.copy(isSavingEditor = false) }
                _messages.send(e.message)
            }
        }
    }

    fun signOut() {
        if (_state.value.isSigningOut) return
        _state.update { it.copy(isSigningOut = true) }
        viewModelScope.launch { auth.signOut() }
    }
}
