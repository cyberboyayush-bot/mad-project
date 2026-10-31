package me.aysh.pocketledger.ui.expense

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import me.aysh.pocketledger.R
import me.aysh.pocketledger.data.model.Category
import me.aysh.pocketledger.ui.appViewModel
import me.aysh.pocketledger.ui.components.ErrorBanner
import me.aysh.pocketledger.ui.components.Eyebrow
import me.aysh.pocketledger.ui.components.FieldShape
import me.aysh.pocketledger.ui.components.LedgerTextField
import me.aysh.pocketledger.ui.components.PrimaryButton
import me.aysh.pocketledger.ui.theme.Spacing
import me.aysh.pocketledger.util.Dates
import me.aysh.pocketledger.util.Validation

@Composable
fun ExpenseEditorScreen(expenseId: String?, onClose: () -> Unit) {
    val viewModel = appViewModel(key = "editor-${expenseId ?: "new"}") { ExpenseEditorViewModel(expenseId, it.ledgerRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isDone) { if (state.isDone) onClose() }

    ExpenseEditorContent(
        state = state,
        onClose = onClose,
        onAmountChange = viewModel::onAmountChange,
        onCategoryChange = viewModel::onCategoryChange,
        onNoteChange = viewModel::onNoteChange,
        onDateChange = viewModel::onDateChange,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
    )
}

@Composable
private fun ExpenseEditorContent(
    state: ExpenseEditorUiState,
    onClose: () -> Unit,
    onAmountChange: (String) -> Unit,
    onCategoryChange: (Category) -> Unit,
    onNoteChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val amountFocus = remember { FocusRequester() }
    val editable = !state.isBusy

    // New expenses start with the keyboard up, ready for the amount.
    LaunchedEffect(Unit) { if (!state.isEditing) amountFocus.requestFocus() }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        EditorTopBar(
            title = stringResource(if (state.isEditing) R.string.editor_edit_title else R.string.editor_new_title),
            showDelete = state.isEditing,
            deleteEnabled = editable,
            onClose = onClose,
            onDelete = { confirmDelete = true },
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(Spacing.xl))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Eyebrow(stringResource(R.string.editor_amount))
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 88.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                } else {
                    AmountInput(
                        value = state.amount,
                        onValueChange = onAmountChange,
                        isError = state.amountError != null,
                        enabled = editable,
                        onDone = { focus.clearFocus() },
                        modifier = Modifier
                            .padding(horizontal = Spacing.gutter)
                            .focusRequester(amountFocus)
                            .testTag("editor_amount"),
                    )
                }
            }
            AnimatedVisibility(state.amountError != null, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    state.amountError.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.xl)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }

            Spacer(Modifier.height(Spacing.xxl))
            Eyebrow(stringResource(R.string.editor_category), Modifier.padding(horizontal = Spacing.gutter))
            Spacer(Modifier.height(Spacing.md))
            CategoryPicker(
                selected = state.category,
                onSelect = onCategoryChange,
                enabled = editable,
                modifier = Modifier.padding(horizontal = Spacing.lg),
            )

            Spacer(Modifier.height(Spacing.xl))
            Eyebrow(stringResource(R.string.editor_details), Modifier.padding(horizontal = Spacing.gutter))
            Spacer(Modifier.height(Spacing.md))
            Column(Modifier.padding(horizontal = Spacing.lg)) {
                LedgerTextField(
                    value = state.note,
                    onValueChange = onNoteChange,
                    label = stringResource(R.string.editor_note),
                    enabled = editable,
                    supportingText = "${state.note.length}/${Validation.MAX_NOTE}",
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Notes, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    modifier = Modifier.testTag("editor_note"),
                )
                Spacer(Modifier.height(Spacing.xs))
                DateRow(
                    date = state.date,
                    enabled = editable,
                    onClick = {
                        focus.clearFocus()
                        showDatePicker = true
                    },
                )
            }
            Spacer(Modifier.height(Spacing.xl))
        }

        Column(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
            ErrorBanner(state.error, Modifier.padding(bottom = Spacing.md))
            PrimaryButton(
                text = stringResource(if (state.isEditing) R.string.editor_update else R.string.editor_save),
                onClick = {
                    focus.clearFocus()
                    onSave()
                },
                enabled = !state.isLoading && !state.isDeleting,
                loading = state.isSaving,
                modifier = Modifier.testTag("editor_save_expense"),
            )
        }
    }

    if (showDatePicker) {
        ExpenseDatePicker(date = state.date, onPick = onDateChange, onDismiss = { showDatePicker = false })
    }
    if (confirmDelete) {
        DeleteExpenseDialog(
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun EditorTopBar(
    title: String,
    showDelete: Boolean,
    deleteEnabled: Boolean,
    onClose: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose, modifier = Modifier.testTag("editor_close")) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.editor_close))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        if (showDelete) {
            IconButton(onClick = onDelete, enabled = deleteEnabled, modifier = Modifier.testTag("editor_delete")) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = stringResource(R.string.editor_delete), tint = MaterialTheme.colorScheme.error)
            }
        } else {
            Spacer(Modifier.width(48.dp))
        }
    }
}

/** Looks like a text field but opens the date picker. */
@Composable
private fun DateRow(date: LocalDate, enabled: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(FieldShape)
            .border(1.dp, scheme.outline, FieldShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.md)
            .testTag("editor_date"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.editor_date), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            Text(Dates.relativeDay(date), style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = scheme.onSurfaceVariant)
    }
}
