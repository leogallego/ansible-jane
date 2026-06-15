package io.github.leogallego.ansiblejane.assistant.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable

@OptIn(ExperimentalLayoutApi::class)
@Composable
actual fun isImeVisible(): Boolean = WindowInsets.isImeVisible
