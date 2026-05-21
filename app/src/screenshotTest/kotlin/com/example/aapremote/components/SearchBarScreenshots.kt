package com.example.aapremote.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.example.aapremote.ui.components.SearchBar
import com.example.aapremote.ui.theme.AapRemoteTheme

@PreviewTest
@Preview(showBackground = true, name = "Search Bar Empty - Light")
@Composable
fun SearchBarEmptyLight() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
        SearchBar(onSearch = {})
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Search Bar Empty - Dark")
@Composable
fun SearchBarEmptyDark() {
    AapRemoteTheme(darkTheme = true, dynamicColor = false) {
        SearchBar(onSearch = {})
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Search Bar With Query")
@Composable
fun SearchBarWithQuery() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
        SearchBar(
            onSearch = {},
            initialQuery = "deploy production"
        )
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Search Bar Custom Placeholder")
@Composable
fun SearchBarCustomPlaceholder() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
        SearchBar(
            onSearch = {},
            placeholder = "Search hosts..."
        )
    }
}

@PreviewTest
@Preview(showBackground = true, fontScale = 1.5f, name = "Search Bar - Large Font")
@Composable
fun SearchBarLargeFont() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
        SearchBar(onSearch = {})
    }
}
