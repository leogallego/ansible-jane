package io.github.leogallego.ansiblejane.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun PaginationEffect(
    listState: LazyListState,
    itemCount: Int,
    threshold: Int = 3,
    onLoadMore: () -> Unit
) {
    LaunchedEffect(listState) {
        snapshotFlow {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            itemCount > threshold && lastVisibleItem >= itemCount - threshold
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { onLoadMore() }
    }
}
