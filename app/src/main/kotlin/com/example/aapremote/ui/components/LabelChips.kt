package com.example.aapremote.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.aapremote.model.Label

@Composable
fun LabelChips(
    labels: List<Label>,
    selectedLabel: Label?,
    onLabelSelected: (Label?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (labels.isEmpty()) return

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEach { label ->
            FilterChip(
                selected = selectedLabel?.id == label.id,
                onClick = {
                    onLabelSelected(if (selectedLabel?.id == label.id) null else label)
                },
                label = { Text(label.name) }
            )
        }
    }
}
