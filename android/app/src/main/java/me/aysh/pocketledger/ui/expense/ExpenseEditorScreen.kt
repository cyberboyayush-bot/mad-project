package me.aysh.pocketledger.ui.expense

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun ExpenseEditorScreen(expenseId: String?, onClose: () -> Unit) {
    Text("Editor $expenseId")
}
