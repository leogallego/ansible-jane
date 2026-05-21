package com.example.aapremote.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aapremote.data.TokenManager
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit = {},
    content: @Composable (TopLevelTab, Segment) -> Unit
) {
    val tokenManager: TokenManager = koinInject()
    val activeInstance by tokenManager.activeInstance.collectAsStateWithLifecycle()

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = TopLevelTab.entries
    val selectedTab = tabs[selectedTabIndex]

    val defaultSegmentIndex = selectedTab.segments.indexOfFirst { it.isDefault }.coerceAtLeast(0)
    var selectedSegmentIndices by rememberSaveable {
        mutableStateOf(mapOf<String, Int>())
    }
    val currentSegmentIndex = selectedSegmentIndices[selectedTab.route] ?: defaultSegmentIndex
    val currentSegment = selectedTab.segments[currentSegmentIndex]

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedContent(
                        targetState = selectedTab.label,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "titleCrossfade"
                    ) { label ->
                        Column {
                            Text(label)
                            activeInstance?.let { instance ->
                                Text(
                                    text = instance.displayLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Notifications coming soon")
                            }
                        },
                        modifier = Modifier.testTag("button_notifications")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications"
                        )
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("button_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                windowInsets = NavigationBarDefaults.windowInsets.union(WindowInsets.ime)
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        modifier = Modifier.testTag("nav_${tab.route.substringAfterLast("/")}"),
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        icon = {
                            Icon(
                                imageVector = if (selectedTabIndex == index) tab.selectedIcon else tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(
                                tab.label,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (selectedTab.segments.size > 1) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    selectedTab.segments.forEachIndexed { index, segment ->
                        SegmentedButton(
                            modifier = Modifier.testTag("segment_${segment.label.lowercase().replace(" ", "_")}"),
                            selected = currentSegmentIndex == index,
                            onClick = {
                                selectedSegmentIndices = selectedSegmentIndices +
                                    (selectedTab.route to index)
                            },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = selectedTab.segments.size
                            )
                        ) {
                            Text(segment.label)
                        }
                    }
                }
            }

            Crossfade(
                targetState = "${selectedTab.route}/${currentSegment.label}",
                label = "segment_crossfade"
            ) { key ->
                // key used to trigger crossfade on tab/segment change
                content(selectedTab, currentSegment)
            }
        }
    }
}
