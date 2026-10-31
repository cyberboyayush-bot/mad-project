package me.aysh.pocketledger.ui.auth

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit, onSignIn: () -> Unit) {
    Text("Welcome")
}

@Composable
fun AuthScreen(mode: AuthMode, onBack: () -> Unit, onSwitchMode: () -> Unit) {
    Text("Auth $mode")
}
