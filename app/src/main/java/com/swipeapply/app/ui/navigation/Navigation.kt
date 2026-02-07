package com.swipeapply.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.swipeapply.app.SupabaseClient
import com.swipeapply.app.data.config.ApiConfig
import com.swipeapply.app.data.model.JobCard
import com.swipeapply.app.data.repository.JobRepository
import com.swipeapply.app.ui.screens.HomeScreen
import com.swipeapply.app.ui.screens.IntroTemplateScreen
import com.swipeapply.app.ui.screens.OnboardingScreen
import com.swipeapply.app.ui.screens.ProfileCreationScreen
import com.swipeapply.app.ui.screens.ProfileScreen
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object ProfileCreation : Screen("profile_creation")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object IntroTemplate : Screen("intro_template/{jobId}") {
        fun createRoute(jobId: String) = "intro_template/$jobId"
    }
}

@Composable
fun SwipeApplyNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Onboarding.route,
    onDarkModeToggle: (Boolean?) -> Unit = {},
    isDarkModeEnabled: Boolean? = null
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = spring(
                    dampingRatio = 0.85f,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = tween(250))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = spring(
                    dampingRatio = 0.85f,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeOut(animationSpec = tween(200))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = spring(
                    dampingRatio = 0.85f,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = tween(250))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = spring(
                    dampingRatio = 0.85f,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeOut(animationSpec = tween(200))
        }
    ) {
        composable(
            route = Screen.Onboarding.route,
            enterTransition = { fadeIn(animationSpec = tween(400)) },
            exitTransition = { fadeOut(animationSpec = tween(250)) }
        ) {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            var isNavigating by remember { mutableStateOf(false) }

            OnboardingScreen(
                onContinue = {
                    if (isNavigating) return@OnboardingScreen
                    isNavigating = true

                    scope.launch {
                        try {
                            val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
                            if (userId != null) {
                                val repository = JobRepository.getInstance(context, ApiConfig.FINDWORK_API_KEY)
                                val hasProfile = repository.hasUserProfile(userId)

                                if (hasProfile) {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                } else {
                                    navController.navigate(Screen.ProfileCreation.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            } else {
                                isNavigating = false
                            }
                        } catch (e: Exception) {
                            isNavigating = false
                        }
                    }
                },
                onSkipLogin = {
                    if (isNavigating) return@OnboardingScreen
                    isNavigating = true
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.ProfileCreation.route
        ) {
            ProfileCreationScreen(
                onNavigateHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.Home.route) {
            HomeScreen(
                onCardClicked = { },
                onRequestIntro = { jobCard ->
                    navController.navigate(Screen.IntroTemplate.createRoute(jobCard.id))
                },
                onDarkModeToggle = onDarkModeToggle,
                isDarkModeEnabled = isDarkModeEnabled,
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onSignOutSuccess = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                        restoreState = false
                    }
                }
            )
        }

        composable(route = Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.IntroTemplate.route,
            arguments = listOf(
                navArgument("jobId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val context = LocalContext.current
            val jobId = backStackEntry.arguments?.getString("jobId") ?: return@composable

            val repository = JobRepository.getInstance(context, ApiConfig.FINDWORK_API_KEY)
            var jobCard by remember { mutableStateOf<JobCard?>(null) }

            LaunchedEffect(jobId) {
                jobCard = repository.getJobCardById(jobId)
            }

            jobCard?.let { card ->
                IntroTemplateScreen(
                    jobCard = card,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
