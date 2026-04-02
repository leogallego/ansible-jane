package com.example.aapremote.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.aapremote.network.AuthInterceptor
import com.example.aapremote.presentation.auth.AuthViewModel
import com.example.aapremote.ui.auth.AuthScreen
import com.example.aapremote.ui.jobs.JobStatusScreen
import com.example.aapremote.ui.jobs.RecentJobsScreen
import com.example.aapremote.ui.templates.TemplateListScreen
import org.koin.compose.viewmodel.koinViewModel

object Routes {
    const val AUTH = "auth"
    const val TEMPLATES = "templates"
    const val JOB_STATUS = "job_status/{jobId}"
    const val RECENT_JOBS = "recent_jobs"

    fun jobStatus(jobId: Int) = "job_status/$jobId"
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
        composable(Routes.AUTH) {
            AuthScreen(
                onNavigateToDashboard = {
                    navController.navigate(Routes.TEMPLATES) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.TEMPLATES) {
            val authViewModel: AuthViewModel = koinViewModel()
            TemplateListScreen(
                onNavigateToJobStatus = { jobId ->
                    navController.navigate(Routes.jobStatus(jobId))
                },
                onNavigateToRecentJobs = {
                    navController.navigate(Routes.RECENT_JOBS)
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.JOB_STATUS,
            arguments = listOf(navArgument("jobId") { type = NavType.IntType })
        ) {
            JobStatusScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.RECENT_JOBS) {
            RecentJobsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToJobStatus = { jobId ->
                    navController.navigate(Routes.jobStatus(jobId))
                }
            )
        }
    }
}
