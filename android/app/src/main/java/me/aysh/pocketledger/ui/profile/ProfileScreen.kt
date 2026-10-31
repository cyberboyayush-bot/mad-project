package me.aysh.pocketledger.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import me.aysh.pocketledger.BuildConfig
import me.aysh.pocketledger.R
import me.aysh.pocketledger.data.model.Profile
import me.aysh.pocketledger.ui.components.ErrorState
import me.aysh.pocketledger.ui.components.Eyebrow
import me.aysh.pocketledger.ui.components.Hairline
import me.aysh.pocketledger.ui.components.SkeletonBlock
import me.aysh.pocketledger.ui.theme.LedgerTheme
import me.aysh.pocketledger.ui.theme.MoneyStyles
import me.aysh.pocketledger.ui.theme.Spacing
import me.aysh.pocketledger.util.Money

@Composable
fun ProfileScreen(viewModel: ProfileViewModel, contentPadding: PaddingValues) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }
    val profile = state.profile

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = contentPadding.calculateBottomPadding() + Spacing.xl)
            .testTag("profile_screen"),
    ) {
        when {
            profile != null -> ProfileBody(
                profile = profile,
                isSigningOut = state.isSigningOut,
                onEditBudget = viewModel::editBudget,
                onEditName = viewModel::editName,
                onSignOut = { confirmSignOut = true },
            )
            state.error != null -> ErrorState(state.error.orEmpty(), viewModel::load, Modifier.padding(top = 120.dp))
            else -> ProfileSkeleton()
        }
    }

    state.editor?.let { editor ->
        ProfileEditorSheet(
            editor = editor,
            isSaving = state.isSavingEditor,
            onValueChange = viewModel::onEditorChange,
            onSave = viewModel::saveEditor,
            onDismiss = viewModel::dismissEditor,
        )
    }
    if (confirmSignOut) {
        SignOutDialog(
            onConfirm = {
                confirmSignOut = false
                viewModel.signOut()
            },
            onDismiss = { confirmSignOut = false },
        )
    }
}

private val memberSinceFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

private fun initials(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

@Composable
private fun ProfileBody(
    profile: Profile,
    isSigningOut: Boolean,
    onEditBudget: () -> Unit,
    onEditName: () -> Unit,
    onSignOut: () -> Unit,
) {
    val colors = LedgerTheme.colors
    Column(Modifier.padding(horizontal = Spacing.gutter).padding(top = Spacing.xxl)) {
        Box(
            Modifier
                .size(80.dp)
                .background(colors.hero, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(initials(profile.name), style = MaterialTheme.typography.headlineLarge, color = colors.onHero)
        }
        Spacer(Modifier.height(Spacing.lg))
        Text(
            profile.name,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            profile.email,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            stringResource(R.string.profile_member_since, profile.memberSince.atZone(ZoneId.systemDefault()).format(memberSinceFormat)),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                .padding(horizontal = Spacing.md, vertical = Spacing.xs + Spacing.xxs),
        )
    }

    Spacer(Modifier.height(Spacing.xxl))
    Eyebrow(stringResource(R.string.profile_section_ledger), Modifier.padding(horizontal = Spacing.gutter, vertical = Spacing.sm))
    RowGroup {
        SettingRow(
            icon = Icons.Rounded.Savings,
            label = stringResource(R.string.profile_budget),
            value = if (profile.monthlyBudgetPaise > 0) Money.format(profile.monthlyBudgetPaise) else stringResource(R.string.profile_budget_none),
            valueIsMoney = profile.monthlyBudgetPaise > 0,
            onClick = onEditBudget,
            modifier = Modifier.testTag("profile_budget"),
        )
        Hairline(Modifier.padding(start = 54.dp))
        SettingRow(
            icon = Icons.Rounded.Badge,
            label = stringResource(R.string.profile_name),
            value = profile.name,
            onClick = onEditName,
            modifier = Modifier.testTag("profile_name"),
        )
        Hairline(Modifier.padding(start = 54.dp))
        SettingRow(
            icon = Icons.Rounded.CurrencyRupee,
            label = stringResource(R.string.profile_currency),
            value = stringResource(R.string.profile_currency_value),
        )
    }

    Spacer(Modifier.height(Spacing.xl))
    Eyebrow(stringResource(R.string.profile_section_about), Modifier.padding(horizontal = Spacing.gutter, vertical = Spacing.sm))
    RowGroup {
        SettingRow(
            icon = Icons.Rounded.Info,
            label = stringResource(R.string.profile_version),
            value = BuildConfig.VERSION_NAME,
        )
    }

    Spacer(Modifier.height(Spacing.xxl))
    OutlinedButton(
        onClick = onSignOut,
        enabled = !isSigningOut,
        shape = CircleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        modifier = Modifier
            .padding(horizontal = Spacing.gutter)
            .fillMaxWidth()
            .height(56.dp)
            .testTag("profile_sign_out"),
    ) {
        if (isSigningOut) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.error)
        } else {
            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(Spacing.sm))
            Text(stringResource(R.string.profile_sign_out), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun RowGroup(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .padding(horizontal = Spacing.lg)
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
    ) { content() }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueIsMoney: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .heightIn(min = 60.dp)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), maxLines = 1)
        Text(
            value,
            style = if (valueIsMoney) MoneyStyles.row else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(max = 168.dp),
        )
        if (onClick != null) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ProfileSkeleton() {
    Column(Modifier.padding(Spacing.gutter).padding(top = Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        SkeletonBlock(Modifier.size(80.dp), height = 80.dp, corner = 40.dp)
        SkeletonBlock(Modifier.fillMaxWidth(0.5f), height = 32.dp)
        SkeletonBlock(Modifier.fillMaxWidth(0.6f))
        Spacer(Modifier.height(Spacing.xl))
        SkeletonBlock(Modifier.fillMaxWidth(), height = 180.dp, corner = 20.dp)
    }
}
