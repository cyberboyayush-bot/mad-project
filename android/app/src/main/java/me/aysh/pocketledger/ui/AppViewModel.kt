package me.aysh.pocketledger.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import me.aysh.pocketledger.AppContainer
import me.aysh.pocketledger.PocketLedgerApp

/** Creates a ViewModel scoped to the current navigation entry, built from the [AppContainer]. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = (LocalContext.current.applicationContext as PocketLedgerApp).container
    return viewModel(key = key) { create(container) }
}
