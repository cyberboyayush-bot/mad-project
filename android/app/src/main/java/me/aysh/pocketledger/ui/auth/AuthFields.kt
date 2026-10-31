package me.aysh.pocketledger.ui.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import me.aysh.pocketledger.R
import me.aysh.pocketledger.ui.components.LedgerTextField
import me.aysh.pocketledger.ui.theme.LedgerTheme
import me.aysh.pocketledger.ui.theme.Spacing

@Composable
internal fun AuthFields(
    state: AuthUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val signUp = state.mode == AuthMode.SignUp
    val focus = LocalFocusManager.current
    val enabled = !state.isSubmitting
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        if (signUp) {
            LedgerTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = stringResource(R.string.auth_name),
                error = state.nameError,
                enabled = enabled,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier
                    .semantics { contentType = ContentType.PersonFullName }
                    .testTag("auth_name"),
            )
        }
        LedgerTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = stringResource(R.string.auth_email),
            error = state.emailError,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next, autoCorrectEnabled = false),
            modifier = Modifier
                .semantics { contentType = if (signUp) ContentType.EmailAddress + ContentType.NewUsername else ContentType.EmailAddress + ContentType.Username }
                .testTag("auth_email"),
        )
        LedgerTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = stringResource(R.string.auth_password),
            error = state.passwordError,
            supportingText = if (signUp) stringResource(R.string.auth_password_hint) else null,
            enabled = enabled,
            visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done, autoCorrectEnabled = false),
            keyboardActions = KeyboardActions(onDone = {
                focus.clearFocus()
                onSubmit()
            }),
            trailingIcon = {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        if (state.passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = stringResource(if (state.passwordVisible) R.string.auth_hide_password else R.string.auth_show_password),
                    )
                }
            },
            modifier = Modifier
                .semantics { contentType = if (signUp) ContentType.NewPassword else ContentType.Password }
                .testTag("auth_password"),
        )
        if (signUp) PasswordStrengthMeter(state.passwordStrength, visible = state.password.isNotEmpty())
    }
}

/** Four short segments that fill and tint as the password gets stronger. */
@Composable
private fun PasswordStrengthMeter(strength: Int, visible: Boolean, modifier: Modifier = Modifier) {
    val colors = LedgerTheme.colors
    val scheme = MaterialTheme.colorScheme
    val tint = when (strength) {
        0, 1 -> colors.caution
        2 -> scheme.tertiary
        3 -> scheme.primary
        else -> colors.positive
    }
    val label = stringResource(
        when (strength) {
            0 -> R.string.strength_0
            1 -> R.string.strength_1
            2 -> R.string.strength_2
            3 -> R.string.strength_3
            else -> R.string.strength_4
        },
    )
    val description = stringResource(R.string.strength_label, label)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xs)
            .clearAndSetSemantics { if (visible) contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(4) { i ->
            val on = visible && i < strength.coerceAtLeast(1)
            val color by animateColorAsState(if (on) tint else scheme.outlineVariant, tween(250), label = "segment")
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(color, CircleShape),
            )
            if (i < 3) Spacer(Modifier.width(Spacing.xs + Spacing.xxs))
        }
        Spacer(Modifier.width(Spacing.md))
        Text(
            text = if (visible) label else "",
            style = MaterialTheme.typography.labelMedium,
            color = if (visible) tint else Color.Transparent,
            modifier = Modifier.width(64.dp),
        )
    }
}
