package io.github.leogallego.ansiblejane.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.leogallego.ansiblejane.ui.theme.AnsibleJaneTheme

@Composable
fun LoadingList(
    count: Int = 5,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(count) { SkeletonCard() }
    }
}

@Preview(showBackground = true, name = "Loading List")
@Composable
private fun LoadingListPreview() {
    AnsibleJaneTheme(dynamicColor = false) {
        LoadingList()
    }
}
