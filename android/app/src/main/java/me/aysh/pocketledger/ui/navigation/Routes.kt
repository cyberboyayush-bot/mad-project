package me.aysh.pocketledger.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object Welcome : NavKey

@Serializable
data object SignIn : NavKey

@Serializable
data object SignUp : NavKey

/** The signed-in shell with the bottom navigation bar. */
@Serializable
data object Main : NavKey

/** Add a new expense when [expenseId] is null, otherwise edit that expense. */
@Serializable
data class ExpenseEditor(val expenseId: String? = null) : NavKey

enum class Tab { Home, History, Stats, Profile }
