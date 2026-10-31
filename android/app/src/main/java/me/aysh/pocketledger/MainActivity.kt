package me.aysh.pocketledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import me.aysh.pocketledger.data.session.SessionState
import me.aysh.pocketledger.ui.RequestLocalNetworkAccess
import me.aysh.pocketledger.ui.navigation.AppNavigation
import me.aysh.pocketledger.ui.theme.PocketLedgerTheme

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val sessionStore = (application as PocketLedgerApp).container.sessionStore
        installSplashScreen().setKeepOnScreenCondition { sessionStore.state.value is SessionState.Loading }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            PocketLedgerTheme {
                RequestLocalNetworkAccess()
                val session by sessionStore.state.collectAsStateWithLifecycle()
                // Exposes test tags as resource ids so UI automation can find elements.
                Box(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
                    if (session !is SessionState.Loading) {
                        AppNavigation(signedIn = session is SessionState.SignedIn)
                    }
                }
            }
        }
    }
}
