package me.aysh.pocketledger.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.aysh.pocketledger.R
import me.aysh.pocketledger.ui.appViewModel
import me.aysh.pocketledger.ui.history.HistoryEvent
import me.aysh.pocketledger.ui.history.HistoryScreen
import me.aysh.pocketledger.ui.history.HistoryViewModel
import me.aysh.pocketledger.ui.home.HomeScreen
import me.aysh.pocketledger.ui.home.HomeViewModel
import me.aysh.pocketledger.ui.navigation.Tab
import me.aysh.pocketledger.ui.profile.ProfileScreen
import me.aysh.pocketledger.ui.profile.ProfileViewModel
import me.aysh.pocketledger.ui.stats.StatsScreen
import me.aysh.pocketledger.ui.stats.StatsViewModel
import me.aysh.pocketledger.ui.theme.Spacing

/**
 * Signed-in shell. The four tab ViewModels are scoped to this nav entry so they survive tab
 * switches; each tab's UI state (scroll position etc.) is kept by a saveable state holder.
 */
@Composable
fun MainScreen(onAddExpense: () -> Unit, onOpenExpense: (String) -> Unit) {
    val homeViewModel = appViewModel { HomeViewModel(it.ledgerRepository) }
    val historyViewModel = appViewModel { HistoryViewModel(it.ledgerRepository) }
    val statsViewModel = appViewModel { StatsViewModel(it.ledgerRepository) }
    val profileViewModel = appViewModel { ProfileViewModel(it.ledgerRepository, it.authRepository) }

    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    val stateHolder = rememberSaveableStateHolder()
    val snackbar = remember { SnackbarHostState() }

    BackHandler(enabled = tab != Tab.Home) { tab = Tab.Home }

    // One-off events are collected here so they're shown even if the user has switched tabs.
    val deletedMessage = stringResource(R.string.history_deleted)
    val undoLabel = stringResource(R.string.action_undo)
    LaunchedEffect(historyViewModel) {
        historyViewModel.events.collect { event ->
            snackbar.currentSnackbarData?.dismiss()
            launch {
                when (event) {
                    is HistoryEvent.Deleted -> {
                        val result = snackbar.showSnackbar(deletedMessage, actionLabel = undoLabel, duration = SnackbarDuration.Long)
                        if (result == SnackbarResult.ActionPerformed) historyViewModel.undoDelete(event.expense)
                    }
                    is HistoryEvent.Failed -> snackbar.showSnackbar(event.message)
                }
            }
        }
    }
    LaunchedEffect(profileViewModel) {
        profileViewModel.messages.collect { message ->
            snackbar.currentSnackbarData?.dismiss()
            launch { snackbar.showSnackbar(message) }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(14.dp),
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    actionColor = MaterialTheme.colorScheme.inversePrimary,
                    modifier = Modifier.padding(horizontal = Spacing.sm),
                )
            }
        },
        bottomBar = { LedgerBottomBar(current = tab, onSelect = { tab = it }, onAdd = onAddExpense) },
    ) { padding ->
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                (fadeIn(tween(240, delayMillis = 60)) + scaleIn(tween(240, delayMillis = 60), initialScale = 0.985f)) togetherWith
                    fadeOut(tween(110))
            },
            label = "tabs",
            modifier = Modifier.statusBarsPadding(),
        ) { current ->
            stateHolder.SaveableStateProvider(current.name) {
                when (current) {
                    Tab.Home -> HomeScreen(
                        viewModel = homeViewModel,
                        contentPadding = padding,
                        onOpenExpense = onOpenExpense,
                        onAddExpense = onAddExpense,
                        onSeeAll = { tab = Tab.History },
                        onSetBudget = {
                            profileViewModel.editBudget()
                            tab = Tab.Profile
                        },
                    )
                    Tab.History -> HistoryScreen(historyViewModel, contentPadding = padding, onOpenExpense = onOpenExpense)
                    Tab.Stats -> StatsScreen(statsViewModel, contentPadding = padding)
                    Tab.Profile -> ProfileScreen(profileViewModel, contentPadding = padding)
                }
            }
        }
    }
}
