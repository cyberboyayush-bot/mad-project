package me.aysh.pocketledger.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.aysh.pocketledger.R
import me.aysh.pocketledger.ui.appViewModel
import me.aysh.pocketledger.ui.components.BrandMark
import me.aysh.pocketledger.ui.components.ErrorBanner
import me.aysh.pocketledger.ui.components.Eyebrow
import me.aysh.pocketledger.ui.components.PrimaryButton
import me.aysh.pocketledger.ui.components.SecondaryButton
import me.aysh.pocketledger.ui.components.rememberEntrance
import me.aysh.pocketledger.ui.components.reveal
import me.aysh.pocketledger.ui.theme.PocketLedgerTheme
import me.aysh.pocketledger.ui.theme.Spacing

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit, onSignIn: () -> Unit) {
    val entered = rememberEntrance()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.xl),
    ) {
        Row(
            modifier = Modifier
                .padding(top = Spacing.lg)
                .reveal(entered, 0),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandMark(size = 34.dp)
            Spacer(Modifier.width(Spacing.md))
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        WelcomeArt(
            visible = entered,
            modifier = Modifier
                .padding(vertical = Spacing.xl)
                .reveal(entered, 1, distance = 32),
        )
        Eyebrow(stringResource(R.string.welcome_eyebrow), Modifier.reveal(entered, 2))
        Spacer(Modifier.height(Spacing.md))
        val line1 = stringResource(R.string.welcome_headline_1)
        val line2 = stringResource(R.string.welcome_headline_2)
        val accent = MaterialTheme.colorScheme.primary
        Text(
            text = buildAnnotatedString {
                append(line1)
                append('\n')
                withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = accent)) { append(line2) }
            },
            style = MaterialTheme.typography.displayMedium.copy(lineHeight = 50.sp),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .reveal(entered, 3)
                .semantics { heading() },
        )
        Spacer(Modifier.height(Spacing.lg))
        Text(
            stringResource(R.string.welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.reveal(entered, 4),
        )
        Spacer(Modifier.height(Spacing.xxl))
        Column(Modifier.reveal(entered, 5), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            PrimaryButton(
                text = stringResource(R.string.welcome_get_started),
                onClick = onGetStarted,
                modifier = Modifier.testTag("welcome_get_started"),
            )
            SecondaryButton(
                text = stringResource(R.string.welcome_sign_in),
                onClick = onSignIn,
                modifier = Modifier.testTag("welcome_sign_in"),
            )
        }
        Spacer(Modifier.height(Spacing.xl))
    }
}

@Composable
fun AuthScreen(mode: AuthMode, onBack: () -> Unit, onSwitchMode: () -> Unit) {
    val viewModel = appViewModel(key = mode.name) { AuthViewModel(mode, it.authRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    AuthContent(
        state = state,
        onBack = onBack,
        onSwitchMode = onSwitchMode,
        onNameChange = viewModel::onNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePassword = viewModel::togglePasswordVisibility,
        onSubmit = viewModel::submit,
    )
}

@Composable
private fun AuthContent(
    state: AuthUiState,
    onBack: () -> Unit,
    onSwitchMode: () -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onSubmit: () -> Unit,
) {
    val signUp = state.mode == AuthMode.SignUp
    val entered = rememberEntrance()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(start = Spacing.sm, top = Spacing.sm)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
        }
        Column(Modifier.padding(horizontal = Spacing.xl)) {
            Spacer(Modifier.height(Spacing.lg))
            Eyebrow(
                stringResource(if (signUp) R.string.auth_signup_eyebrow else R.string.auth_signin_eyebrow),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.reveal(entered, 0),
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                stringResource(if (signUp) R.string.auth_signup_title else R.string.auth_signin_title),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .reveal(entered, 1)
                    .semantics { heading() },
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                stringResource(if (signUp) R.string.auth_signup_subtitle else R.string.auth_signin_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.reveal(entered, 2),
            )
            Spacer(Modifier.height(Spacing.xxl))
            AuthFields(
                state = state,
                onNameChange = onNameChange,
                onEmailChange = onEmailChange,
                onPasswordChange = onPasswordChange,
                onTogglePassword = onTogglePassword,
                onSubmit = onSubmit,
                modifier = Modifier.reveal(entered, 3),
            )
            ErrorBanner(
                message = state.formError,
                modifier = Modifier
                    .padding(top = Spacing.lg)
                    .testTag("auth_error"),
            )
            Spacer(Modifier.height(Spacing.xl))
            PrimaryButton(
                text = stringResource(if (signUp) R.string.auth_signup_submit else R.string.auth_signin_submit),
                onClick = onSubmit,
                loading = state.isSubmitting,
                modifier = Modifier
                    .reveal(entered, 4)
                    .testTag("auth_submit"),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.lg),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(if (signUp) R.string.auth_switch_to_signin_prompt else R.string.auth_switch_to_signup_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onSwitchMode, modifier = Modifier.testTag("auth_switch_mode")) {
                    Text(
                        stringResource(if (signUp) R.string.auth_switch_to_signin else R.string.auth_switch_to_signup),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun SignUpPreview() {
    PocketLedgerTheme {
        AuthContent(
            state = AuthUiState(AuthMode.SignUp, name = "Ayush", email = "ayush@", password = "secret1", showFieldErrors = true, formError = "That email is already registered"),
            onBack = {}, onSwitchMode = {}, onNameChange = {}, onEmailChange = {}, onPasswordChange = {}, onTogglePassword = {}, onSubmit = {},
        )
    }
}

@Preview
@Composable
private fun WelcomeDarkPreview() {
    PocketLedgerTheme(darkTheme = true) { WelcomeScreen(onGetStarted = {}, onSignIn = {}) }
}
