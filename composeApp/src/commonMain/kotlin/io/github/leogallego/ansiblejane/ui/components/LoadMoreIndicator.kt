package io.github.leogallego.ansiblejane.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoadMoreIndicator(
    modifier: Modifier = Modifier
) {
    LinearProgressIndicator(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}
