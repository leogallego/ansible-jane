package io.github.leogallego.ansiblejane.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.leogallego.ansiblejane.assistant.data.IAssistantRepository
import io.github.leogallego.ansiblejane.R
import io.github.leogallego.ansiblejane.data.TokenManager
import io.github.leogallego.ansiblejane.presentation.notifications.NotificationsViewModel
import io.github.leogallego.ansiblejane.ui.components.ProviderSwitchChip
import io.github.leogallego.ansiblejane.ui.notifications.NotificationsSheet
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToApproval: (Int) -> Unit = {},
    content: @Composable (TopLevelTab, Segment) -> Unit
) {
    val tokenManager: TokenManager = koinInject()
    val assistantRepository: IAssistantRepository = koinInject()
    val activeInstance by tokenManager.activeInstance.collectAsStateWithLifecycle()
    val activeConfig by assistantRepository.activeConfigFlow.collectAsStateWithLifecycle(null)
    val savedConfigs by assistantRepository.savedConfigsFlow.collectAsStateWithLifecycle(emptyMap())
    val activeProviderKey by assistantRepository.activeProviderKeyFlow.collectAsStateWithLifecycle(null)
    val sessionTokens by assistantRepository.sessionTokensFlow.collectAsStateWithLifecycle()

    val tabs = TopLevelTab.entries
    val dashboardTabIndex = tabs.indexOfFirst { it is TopLevelTab.Dashboard }.coerceAtLeast(0)
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(dashboardTabIndex) }
    val selectedTab = tabs[selectedTabIndex]

    val defaultSegmentIndex = selectedTab.segments.indexOfFirst { it.isDefault }.coerceAtLeast(0)
    var selectedSegmentIndices by rememberSaveable {
        mutableStateOf(mapOf<String, Int>())
    }
    val currentSegmentIndex = selectedSegmentIndices[selectedTab.route] ?: defaultSegmentIndex
    val currentSegment = selectedTab.segments[currentSegmentIndex]

    val notificationsViewModel: NotificationsViewModel = koinViewModel()
    val notificationsState by notificationsViewModel.uiState.collectAsStateWithLifecycle()
    var showNotificationsSheet by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showClearChatConfirm by remember { mutableStateOf(false) }

    if (showClearChatConfirm) {
        AlertDialog(
            onDismissRequest = { showClearChatConfirm = false },
            title = { Text(stringResource(R.string.clear_chat_title)) },
            text = { Text(stringResource(R.string.clear_chat_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearChatConfirm = false
                    assistantRepository.clearHistory()
                }) { Text(stringResource(R.string.clear_chat_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "titleCrossfade"
                    ) { tab ->
                        Column {
                            Text(stringResource(tab.labelResId))
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
                    ProviderSwitchChip(
                        activeProviderKey = activeProviderKey,
                        activeConfig = activeConfig,
                        savedConfigs = savedConfigs,
                        onSwitchProvider = { key ->
                            scope.launch {
                                assistantRepository.switchActiveProvider(key)
                            }
                        },
                        onNavigateToSettings = onNavigateToSettings
                    )
                    if (selectedTab is TopLevelTab.Assistant && sessionTokens > 0) {
                        val formatted = io.github.leogallego.ansiblejane.assistant.engine.TokenUsage(
                            0, 0, sessionTokens
                        ).formatTotal()
                        Text(
                            text = "$formatted tokens",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .testTag("text_session_tokens"),
                        )
                    }
                    if (selectedTab is TopLevelTab.Assistant) {
                        IconButton(
                            onClick = { showClearChatConfirm = true },
                            modifier = Modifier.testTag("button_clear_chat")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = stringResource(R.string.cd_clear_chat)
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            notificationsViewModel.refreshIfStale()
                            showNotificationsSheet = true
                        },
                        modifier = Modifier.testTag("button_notifications")
                    ) {
                        BadgedBox(
                            badge = {
                                val count = notificationsState.pendingCount
                                if (count > 0) {
                                    Badge { Text(count.toString()) }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = stringResource(R.string.cd_notifications)
                            )
                        }
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("button_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.cd_settings)
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        modifier = Modifier.testTag("nav_${tab.route.substringAfterLast("/")}"),
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        icon = {
                            Icon(
                                imageVector = if (selectedTabIndex == index) tab.selectedIcon else tab.icon,
                                contentDescription = stringResource(tab.labelResId)
                            )
                        },
                        label = {
                            Text(
                                stringResource(tab.labelResId),
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
                .consumeWindowInsets(padding)
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
                            Text(stringResource(segment.labelResId))
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

    if (showNotificationsSheet) {
        NotificationsSheet(
            uiState = notificationsState,
            onDismiss = { showNotificationsSheet = false },
            onRefresh = { notificationsViewModel.refresh() },
            onApprovalClick = { approvalId ->
                showNotificationsSheet = false
                onNavigateToApproval(approvalId)
            }
        )
    }
}
