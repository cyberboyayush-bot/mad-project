package me.aysh.pocketledger.ui.expense

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import me.aysh.pocketledger.R
import me.aysh.pocketledger.ui.components.exposeTestTags
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Material date picker that refuses future dates; you can only log money already spent. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExpenseDatePicker(
    date: LocalDate,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val today = LocalDate.now()
    val todayMillis = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        yearRange = (today.year - 5)..today.year,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis
            override fun isSelectableYear(year: Int) = year <= today.year
        },
    )
    val scheme = MaterialTheme.colorScheme
    val colors = DatePickerDefaults.colors(
        containerColor = scheme.surfaceContainerLow,
        selectedDayContainerColor = scheme.primary,
        selectedDayContentColor = scheme.onPrimary,
        todayDateBorderColor = scheme.primary,
        todayContentColor = scheme.primary,
        selectedYearContainerColor = scheme.primary,
        selectedYearContentColor = scheme.onPrimary,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.exposeTestTags(),
        colors = colors,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onPick(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    onDismiss()
                },
                enabled = state.selectedDateMillis != null,
            ) { Text(stringResource(R.string.editor_pick_date), style = MaterialTheme.typography.labelLarge) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), style = MaterialTheme.typography.labelLarge, color = scheme.onSurface)
            }
        },
    ) {
        DatePicker(state = state, colors = colors, showModeToggle = false)
    }
}

@Composable
internal fun DeleteExpenseDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.exposeTestTags(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        title = { Text(stringResource(R.string.editor_delete_title), style = MaterialTheme.typography.headlineSmall) },
        text = { Text(stringResource(R.string.editor_delete_body), style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag("confirm_delete")) {
                Text(stringResource(R.string.history_delete_action), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}
