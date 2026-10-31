package me.aysh.pocketledger.ui.components

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

/**
 * Dialogs and bottom sheets live in their own windows, so the flag set on the activity root
 * doesn't reach them. Apply this to their content so UI automation can find tagged nodes.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.exposeTestTags(): Modifier = semantics { testTagsAsResourceId = true }
