package me.aysh.pocketledger.ui.main

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun MainScreen(onAddExpense: () -> Unit, onOpenExpense: (String) -> Unit) {
    Text("Main")
}
