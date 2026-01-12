package com.swipeapply.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.swipeapply.app.data.model.JobCard
import com.swipeapply.app.data.repository.MockJobRepository
import com.swipeapply.app.ui.screens.HomeScreen
import com.swipeapply.app.ui.screens.IntroTemplateScreen
import com.swipeapply.app.ui.screens.OnboardingScreen

/**
 * Navigation routes
 */
sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object IntroTemplate : Screen("intro_template/{jobId}") {
        fun createRoute(jobId: String) = "intro_template/$jobId"
    }
}

/**
 * Main navigation host for the app.
 * Handles screen transitions with smooth animations.
 */
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
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        // Onboarding screen
        composable(
            route = Screen.Onboarding.route,
            enterTransition = { fadeIn(animationSpec = tween(500)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            OnboardingScreen(
                onContinue = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // Home / Swipe screen
        composable(route = Screen.Home.route) {
            HomeScreen(
                onCardClicked = { /* Handled by bottom sheet */ },
                onRequestIntro = { jobCard ->
                    navController.navigate(Screen.IntroTemplate.createRoute(jobCard.id))
                },
                onDarkModeToggle = onDarkModeToggle,
                isDarkModeEnabled = isDarkModeEnabled,
                // NEW: Handle sign out navigation
                onSignOutSuccess = {
    navController.navigate(Screen.Onboarding.route) {
        // Clear everything – very aggressive
        popUpTo(navController.graph.startDestinationId) { inclusive = true }
        // Alternative (even stronger): popUpTo(0) { inclusive = true }
        launchSingleTop = true
        restoreState = false          // ← Prevents restoring any old state
    }
}
                
            )
        }
        composable(
            route = Screen.IntroTemplate.route,
            arguments = listOf(
                navArgument("jobId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: return@composable
            val jobCard = MockJobRepository.getJobCardById(jobId) ?: return@composable

            IntroTemplateScreen(
                jobCard = jobCard,
                onBack = { navController.popBackStack() }
            )
        }
    }
}