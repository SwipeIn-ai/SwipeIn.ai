package com.swipeapply.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
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
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Navigation routes
 */
sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object ProfileCreation : Screen("profile_creation")
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
        // Onboarding screen (Login/Signup)
        // After successful authentication, checks if profile exists:
        // - If profile exists → go to Home
        // - If no profile → go to ProfileCreation
        composable(
            route = Screen.Onboarding.route,
            enterTransition = { fadeIn(animationSpec = tween(500)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            var isNavigating by remember { mutableStateOf(false) }
            
            OnboardingScreen(
                onContinue = {
                    // Prevent multiple navigations
                    if (isNavigating) return@OnboardingScreen
                    isNavigating = true
                    
                    // After authentication, check if user has profile
                    scope.launch {
                        try {
                            val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
                            if (userId != null) {
                                val repository = JobRepository.getInstance(context, ApiConfig.FINDWORK_API_KEY)
                                val hasProfile = repository.hasUserProfile(userId)
                                
                                if (hasProfile) {
                                    // User already has profile → go to Home
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                } else {
                                    // No profile → go to ProfileCreation
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
                    // Dev mode: Skip authentication entirely and go straight to Home
                    if (isNavigating) return@OnboardingScreen
                    isNavigating = true
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Profile Creation screen (resume upload & verification)
        // Only accessible after user is authenticated
        composable(
            route = Screen.ProfileCreation.route
        ) {
            ProfileCreationScreen(
                onNavigateHome = {
                    // After profile created, go to Home screen
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
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
        
        // Intro template screen
        composable(
            route = Screen.IntroTemplate.route,
            arguments = listOf(
                navArgument("jobId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val context = LocalContext.current
            val jobId = backStackEntry.arguments?.getString("jobId") ?: return@composable
            
            // Get real job from repository
            val repository = JobRepository.getInstance(context, ApiConfig.FINDWORK_API_KEY)
            var jobCard by remember { mutableStateOf<JobCard?>(null) }
            
            LaunchedEffect(jobId) {
                jobCard = repository.getJobCardById(jobId)
            }
            
            // Show loading or the template screen
            jobCard?.let { card ->
                IntroTemplateScreen(
                    jobCard = card,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}