package io.github.leogallego.ansiblejane.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.leogallego.ansiblejane.data.TokenManager
import io.github.leogallego.ansiblejane.network.AuthInterceptor
import io.github.leogallego.ansiblejane.presentation.auth.AuthViewModel
import io.github.leogallego.ansiblejane.ui.auth.AuthScreen
import io.github.leogallego.ansiblejane.ui.jobs.JobStatusScreen
import io.github.leogallego.ansiblejane.ui.main.MainScreen
import io.github.leogallego.ansiblejane.ui.settings.SettingsScreen
import io.github.leogallego.ansiblejane.ui.workflows.WorkflowJobStatusScreen
import io.github.leogallego.ansiblejane.ui.workflows.WorkflowTemplateDetailScreen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val SPLASH = "splash"
    const val AUTH = "auth"
    const val AUTH_ADD_INSTANCE = "auth?mode=add"
    const val AUTH_REAUTH = "auth?mode=reauth&instanceId={instanceId}&url={url}&alias={alias}&trustSelfSigned={trustSelfSigned}"
    const val MAIN = "main"
    const val JOB_STATUS = "job_status/{jobId}"
    const val WORKFLOW_JOB_STATUS = "workflow_job_status/{workflowJobId}"
    const val WORKFLOW_TEMPLATE_DETAIL = "workflow_template_detail/{templateId}/{templateName}"
    const val SETTINGS = "settings"

    fun jobStatus(jobId: Int) = "job_status/$jobId"
    fun workflowJobStatus(workflowJobId: Int) = "workflow_job_status/$workflowJobId"
    fun workflowTemplateDetail(templateId: Int, templateName: String): String {
        val encoded = URLEncoder.encode(templateName, "UTF-8")
        return "workflow_template_detail/$templateId/$encoded"
    }

    fun reAuth(instanceId: String, url: String, alias: String?, trustSelfSigned: Boolean): String {
        val encodedUrl = URLEncoder.encode(url, "UTF-8")
        val encodedAlias = alias?.let { URLEncoder.encode(it, "UTF-8") } ?: ""
        return "auth?mode=reauth&instanceId=$instanceId&url=$encodedUrl&alias=$encodedAlias&trustSelfSigned=$trustSelfSigned"
    }
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val tokenManager: TokenManager = koinInject()

    // Navigate to auth when last instance is removed (skip during splash)
    val instances by tokenManager.instances.collectAsStateWithLifecycle()
    LaunchedEffect(instances) {
        if (instances.isEmpty()) {
            val currentRoute = navController.currentDestination?.route
            if (currentRoute != null
                && !currentRoute.startsWith("auth")
                && currentRoute != Routes.SPLASH
            ) {
                navController.navigate(Routes.AUTH) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    // Per-instance 401 re-auth: navigate to auth screen pre-filled with instance details
    // Only navigate if not already on the auth screen to prevent re-auth loops
    LaunchedEffect(Unit) {
        AuthInterceptor.unauthorizedEvent.collect { instanceId ->
            val currentRoute = navController.currentDestination?.route
            if (currentRoute != null && currentRoute.startsWith("auth")) {
                return@collect // Already on auth screen, skip
            }

            if (instanceId.isNotBlank()) {
                val instance = tokenManager.getInstanceById(instanceId)
                if (instance != null) {
                    navController.navigate(
                        Routes.reAuth(instanceId, instance.baseUrl, instance.alias, instance.trustSelfSigned)
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
        startDestination = Routes.SPLASH,
        modifier = modifier
    ) {
        composable(
            Routes.SPLASH,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            LaunchedEffect(Unit) {
                val hasInstances = tokenManager.loadCredentials()
                val destination = if (hasInstances) Routes.MAIN else Routes.AUTH
                navController.navigate(destination) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            }
        }

        composable(
            route = "auth?mode={mode}&instanceId={instanceId}&url={url}&alias={alias}&trustSelfSigned={trustSelfSigned}",
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType; defaultValue = "" },
                navArgument("instanceId") { type = NavType.StringType; defaultValue = "" },
                navArgument("url") { type = NavType.StringType; defaultValue = "" },
                navArgument("alias") { type = NavType.StringType; defaultValue = "" },
                navArgument("trustSelfSigned") { type = NavType.BoolType; defaultValue = false }
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
            val preFilledTrustSelfSigned = backStackEntry.arguments?.getBoolean("trustSelfSigned") ?: false

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
                onCancel = if (isAddMode || reAuthInstanceId != null) {
                    { navController.popBackStack() }
                } else null,
                preFilledUrl = preFilledUrl,
                preFilledAlias = preFilledAlias,
                reAuthInstanceId = reAuthInstanceId,
                isAddInstance = isAddMode,
                preFilledTrustSelfSigned = preFilledTrustSelfSigned
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
                    },
                    onNavigateToWorkflowTemplateDetail = { templateId, templateName ->
                        navController.navigate(Routes.workflowTemplateDetail(templateId, templateName))
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
            route = Routes.WORKFLOW_TEMPLATE_DETAIL,
            arguments = listOf(
                navArgument("templateId") { type = NavType.IntType },
                navArgument("templateName") { type = NavType.StringType; defaultValue = "" }
            ),
            enterTransition = { slideInHorizontally { it } },
            exitTransition = { slideOutHorizontally { -it } },
            popEnterTransition = { slideInHorizontally { -it } },
            popExitTransition = { slideOutHorizontally { it } }
        ) {
            WorkflowTemplateDetailScreen(
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
