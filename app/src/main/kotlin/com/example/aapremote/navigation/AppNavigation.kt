package com.example.aapremote.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aapremote.data.TokenManager
import com.example.aapremote.network.AuthInterceptor
import com.example.aapremote.presentation.auth.AuthViewModel
import com.example.aapremote.ui.auth.AuthScreen
import com.example.aapremote.ui.jobs.JobStatusScreen
import com.example.aapremote.ui.main.MainScreen
import com.example.aapremote.ui.settings.SettingsScreen
import com.example.aapremote.ui.workflows.WorkflowJobStatusScreen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val AUTH = "auth"
    const val AUTH_ADD_INSTANCE = "auth?mode=add"
    const val AUTH_REAUTH = "auth?mode=reauth&instanceId={instanceId}&url={url}&alias={alias}"
    const val MAIN = "main"
    const val JOB_STATUS = "job_status/{jobId}"
    const val WORKFLOW_JOB_STATUS = "workflow_job_status/{workflowJobId}"
    const val SETTINGS = "settings"

    fun jobStatus(jobId: Int) = "job_status/$jobId"
    fun workflowJobStatus(workflowJobId: Int) = "workflow_job_status/$workflowJobId"

    fun reAuth(instanceId: String, url: String, alias: String?): String {
        val encodedUrl = URLEncoder.encode(url, "UTF-8")
        val encodedAlias = alias?.let { URLEncoder.encode(it, "UTF-8") } ?: ""
        return "auth?mode=reauth&instanceId=$instanceId&url=$encodedUrl&alias=$encodedAlias"
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val tokenManager: TokenManager = koinInject()

    // Navigate to auth when last instance is removed
    val instances by tokenManager.instances.collectAsStateWithLifecycle()
    LaunchedEffect(instances) {
        if (instances.isEmpty()) {
            val currentRoute = navController.currentDestination?.route
            if (currentRoute != null && !currentRoute.startsWith("auth")) {
                navController.navigate(Routes.AUTH) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    // Per-instance 401 re-auth: navigate to auth screen pre-filled with instance details
    LaunchedEffect(Unit) {
        AuthInterceptor.unauthorizedEvent.collect { instanceId ->
            if (instanceId.isNotBlank()) {
                val instance = tokenManager.getInstanceById(instanceId)
                if (instance != null) {
                    navController.navigate(
                        Routes.reAuth(instanceId, instance.baseUrl, instance.alias)
                    )
                    return@collect
                }
            }
            // Fallback: global logout
            navController.navigate(Routes.AUTH) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.AUTH
    ) {
        composable(
            route = "auth?mode={mode}&instanceId={instanceId}&url={url}&alias={alias}",
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType; defaultValue = "" },
                navArgument("instanceId") { type = NavType.StringType; defaultValue = "" },
                navArgument("url") { type = NavType.StringType; defaultValue = "" },
                navArgument("alias") { type = NavType.StringType; defaultValue = "" }
            ),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: ""
            val instanceId = backStackEntry.arguments?.getString("instanceId") ?: ""
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
            val encodedAlias = backStackEntry.arguments?.getString("alias") ?: ""

            val preFilledUrl = if (encodedUrl.isNotBlank()) {
                URLDecoder.decode(encodedUrl, "UTF-8")
            } else null
            val preFilledAlias = if (encodedAlias.isNotBlank()) {
                URLDecoder.decode(encodedAlias, "UTF-8")
            } else null
            val reAuthInstanceId = if (mode == "reauth" && instanceId.isNotBlank()) {
                instanceId
            } else null

            val isAddMode = mode == "add"

            AuthScreen(
                onNavigateToDashboard = {
                    if (isAddMode || reAuthInstanceId != null) {
                        // Return to settings/main after adding or re-auth
                        navController.popBackStack()
                    } else {
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.AUTH) { inclusive = true }
                        }
                    }
                },
                preFilledUrl = preFilledUrl,
                preFilledAlias = preFilledAlias,
                reAuthInstanceId = reAuthInstanceId
            )
        }

        composable(
            Routes.MAIN,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            MainScreen(
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                }
            ) { tab, segment ->
                TabContent(
                    tab = tab,
                    segment = segment,
                    onNavigateToJobStatus = { jobId ->
                        navController.navigate(Routes.jobStatus(jobId))
                    },
                    onNavigateToWorkflowJobStatus = { workflowJobId ->
                        navController.navigate(Routes.workflowJobStatus(workflowJobId))
                    }
                )
            }
        }

        composable(
            route = Routes.JOB_STATUS,
            arguments = listOf(navArgument("jobId") { type = NavType.IntType }),
            enterTransition = { slideInHorizontally { it } },
            exitTransition = { slideOutHorizontally { -it } },
            popEnterTransition = { slideInHorizontally { -it } },
            popExitTransition = { slideOutHorizontally { it } }
        ) {
            JobStatusScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.WORKFLOW_JOB_STATUS,
            arguments = listOf(navArgument("workflowJobId") { type = NavType.IntType }),
            enterTransition = { slideInHorizontally { it } },
            exitTransition = { slideOutHorizontally { -it } },
            popEnterTransition = { slideInHorizontally { -it } },
            popExitTransition = { slideOutHorizontally { it } }
        ) {
            WorkflowJobStatusScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.SETTINGS,
            enterTransition = { slideInHorizontally { it } },
            exitTransition = { slideOutHorizontally { -it } },
            popEnterTransition = { slideInHorizontally { -it } },
            popExitTransition = { slideOutHorizontally { it } }
        ) {
            val authViewModel: AuthViewModel = koinViewModel()
            SettingsScreen(
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() },
                onAddInstance = {
                    navController.navigate(Routes.AUTH_ADD_INSTANCE)
                }
            )
        }
    }
}
