package me.aysh.pocketledger.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import me.aysh.pocketledger.R
import me.aysh.pocketledger.ui.components.LedgerTextField
import me.aysh.pocketledger.ui.components.PrimaryButton
import me.aysh.pocketledger.ui.components.exposeTestTags
import me.aysh.pocketledger.ui.theme.MoneyStyles
import me.aysh.pocketledger.ui.theme.Spacing

/** Bottom sheet that edits either the name or the monthly budget. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileEditorSheet(
    editor: ProfileEditor,
    isSaving: Boolean,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val isBudget = editor is ProfileEditor.Budget
    val (value, error) = when (editor) {
        is ProfileEditor.Name -> editor.value to editor.error
        is ProfileEditor.Budget -> editor.value to editor.error
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.exposeTestTags(),
    ) {
        Column(
            Modifier
                .padding(horizontal = Spacing.xl)
                .padding(bottom = Spacing.xl)
                .imePadding(),
        ) {
            Text(
                stringResource(if (isBudget) R.string.profile_edit_budget_title else R.string.profile_edit_name_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                stringResource(if (isBudget) R.string.profile_edit_budget_body else R.string.profile_edit_name_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.xl))
            LedgerTextField(
                value = value,
                onValueChange = onValueChange,
                label = stringResource(if (isBudget) R.string.profile_budget else R.string.profile_name),
                error = error,
                enabled = !isSaving,
                textStyle = if (isBudget) MoneyStyles.title else MaterialTheme.typography.bodyLarge,
                prefix = if (isBudget) {
                    { Text("₹ ", style = MoneyStyles.title, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isBudget) KeyboardType.Decimal else KeyboardType.Text,
                    capitalization = if (isBudget) KeyboardCapitalization.None else KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { if (error == null) onSave() }),
                modifier = Modifier
                    .focusRequester(focus)
                    .testTag("editor_field"),
            )
            Spacer(Modifier.height(Spacing.lg))
            PrimaryButton(
                text = stringResource(R.string.action_save),
                onClick = onSave,
                enabled = error == null,
                loading = isSaving,
                modifier = Modifier.testTag("editor_save"),
            )
        }
    }
}

@Composable
internal fun SignOutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.exposeTestTags(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        title = { Text(stringResource(R.string.profile_sign_out_title), style = MaterialTheme.typography.headlineSmall) },
        text = { Text(stringResource(R.string.profile_sign_out_body), style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag("confirm_sign_out")) {
                Text(stringResource(R.string.profile_sign_out), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}
