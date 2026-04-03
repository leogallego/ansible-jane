package com.example.aapremote.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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

object Routes {
    const val AUTH = "auth"
    const val MAIN = "main"
    const val JOB_STATUS = "job_status/{jobId}"
    const val WORKFLOW_JOB_STATUS = "workflow_job_status/{workflowJobId}"
    const val SETTINGS = "settings"

    fun jobStatus(jobId: Int) = "job_status/$jobId"
    fun workflowJobStatus(workflowJobId: Int) = "workflow_job_status/$workflowJobId"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    LaunchedEffect(Unit) {
        AuthInterceptor.unauthorizedEvent.collect {
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
            Routes.AUTH,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            AuthScreen(
                onNavigateToDashboard = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
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
            val tokenManager: TokenManager = koinInject()
            SettingsScreen(
                serverUrl = tokenManager.cachedBaseUrl ?: "Unknown",
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
