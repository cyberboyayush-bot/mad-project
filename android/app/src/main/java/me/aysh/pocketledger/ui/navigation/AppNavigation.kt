package me.aysh.pocketledger.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import me.aysh.pocketledger.ui.auth.AuthMode
import me.aysh.pocketledger.ui.auth.AuthScreen
import me.aysh.pocketledger.ui.auth.WelcomeScreen
import me.aysh.pocketledger.ui.expense.ExpenseEditorScreen
import me.aysh.pocketledger.ui.main.MainScreen

/**
 * Root navigation. The back stack is reset whenever the user signs in or out, so the
 * auth screens and the app screens never mix and Back can't return to a stale session.
 */
@Composable
fun AppNavigation(signedIn: Boolean) {
    val root: NavKey = if (signedIn) Main else Welcome
    val backStack = rememberNavBackStack(root)

    LaunchedEffect(signedIn) {
        if (backStack.firstOrNull() != root) {
            backStack.clear()
            backStack.add(root)
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        transitionSpec = NavTransitions.push,
        popTransitionSpec = NavTransitions.pop,
        predictivePopTransitionSpec = NavTransitions.predictivePop,
        entryProvider = entryProvider {
            entry<Welcome> {
                WelcomeScreen(
                    onGetStarted = { backStack.add(SignUp) },
                    onSignIn = { backStack.add(SignIn) },
                )
            }
            entry<SignIn> { key ->
                AuthScreen(
                    mode = AuthMode.SignIn,
                    onBack = { backStack.popIfTop(key) },
                    onSwitchMode = { backStack.replaceTop(key, SignUp) },
                )
            }
            entry<SignUp> { key ->
                AuthScreen(
                    mode = AuthMode.SignUp,
                    onBack = { backStack.popIfTop(key) },
                    onSwitchMode = { backStack.replaceTop(key, SignIn) },
                )
            }
            entry<Main> {
                MainScreen(
                    onAddExpense = { backStack.add(ExpenseEditor()) },
                    onOpenExpense = { id -> backStack.add(ExpenseEditor(id)) },
                )
            }
            entry<ExpenseEditor> { key ->
                ExpenseEditorScreen(
                    expenseId = key.expenseId,
                    onClose = { backStack.popIfTop(key) },
                )
            }
        },
    )
}

/** Pops [key] only if it's still on top, so double taps during a transition can't empty the stack. */
private fun MutableList<NavKey>.popIfTop(key: NavKey) {
    if (size > 1 && last() == key) removeAt(lastIndex)
}

private fun MutableList<NavKey>.replaceTop(current: NavKey, next: NavKey) {
    if (lastOrNull() != current) return
    removeAt(lastIndex)
    add(next)
}
