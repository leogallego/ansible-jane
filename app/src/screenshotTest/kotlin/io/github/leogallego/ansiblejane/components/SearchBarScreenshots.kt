package io.github.leogallego.ansiblejane.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import io.github.leogallego.ansiblejane.ui.components.SearchBar
import io.github.leogallego.ansiblejane.ui.theme.AnsibleJaneTheme

@PreviewTest
@Preview(showBackground = true, name = "Search Bar Empty - Light")
@Composable
fun SearchBarEmptyLight() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        SearchBar(onSearch = {})
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Search Bar Empty - Dark")
@Composable
fun SearchBarEmptyDark() {
    AnsibleJaneTheme(darkTheme = true, dynamicColor = false) {
        SearchBar(onSearch = {})
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Search Bar With Query")
@Composable
fun SearchBarWithQuery() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
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
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
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
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        SearchBar(onSearch = {})
    }
}
