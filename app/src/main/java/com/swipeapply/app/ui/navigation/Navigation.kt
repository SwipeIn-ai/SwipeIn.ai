package com.swipeapply.app.ui.navigation

import android.util.Log
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.swipeapply.app.SupabaseClient
import com.swipeapply.app.data.config.ApiConfig
import com.swipeapply.app.data.repository.JobRepository
import com.swipeapply.app.ui.screens.EmployeeFinderScreen
import com.swipeapply.app.ui.screens.HomeScreen
import com.swipeapply.app.ui.screens.MainScaffold
import com.swipeapply.app.ui.screens.OnboardingScreen
import com.swipeapply.app.ui.screens.PrivacyPolicyScreen
import com.swipeapply.app.ui.screens.ProfileCreationScreen
import com.swipeapply.app.ui.screens.ProfileScreen
import com.swipeapply.app.ui.screens.TermsScreen
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object ProfileCreation : Screen("profile_creation")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object PrivacyPolicy : Screen("privacy_policy")
    object Terms : Screen("terms")
    object EmployeeFinder : Screen("employee_finder/{companyName}") {
        fun createRoute(companyName: String): String {
            val encoded = URLEncoder.encode(companyName, "UTF-8")
            return "employee_finder/$encoded"
        }
    }
}

@Composable
fun SwipeApplyNavHost(
    navController: NavHostController = rememberNavController(),
    onDarkModeToggle: (Boolean?) -> Unit = {},
    isDarkModeEnabled: Boolean? = null
) {
    val context = LocalContext.current
    var resolvedStartDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(context) {
        resolvedStartDestination = try {
            SupabaseClient.client.auth.awaitInitialization()
            val userId = SupabaseClient.getCurrentUserId()
            when {
                userId == null -> Screen.Onboarding.route
                else -> {
                    // If profile check fails (e.g. table doesn't exist), go to ProfileCreation
                    val hasProfile = try {
                        JobRepository.getInstance(context, ApiConfig.FINDWORK_API_KEY).hasUserProfile(userId)
                    } catch (e: Exception) {
                        Log.e("Navigation", "Profile check failed, defaulting to ProfileCreation", e)
                        false
                    }
                    if (hasProfile) Screen.Home.route else Screen.ProfileCreation.route
                }
            }
        } catch (e: Exception) {
            Log.e("Navigation", "Auth init failed", e)
            Screen.Onboarding.route
        }
    }

    val startDestination = resolvedStartDestination
    if (startDestination == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

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
                            // Get user ID: prefer currentUser, fallback to decoding JWT
                            val userId = SupabaseClient.getCurrentUserId()

                            if (userId != null) {
                                val hasProfile = try {
                                    JobRepository.getInstance(context, ApiConfig.FINDWORK_API_KEY)
                                        .hasUserProfile(userId)
                                } catch (_: Exception) { false }

                                if (hasProfile) {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                } else {
                                    navController.navigate(Screen.ProfileCreation.route) {
                                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            } else {
                                Log.e("Navigation", "Failed to get user ID after authentication retries")
                                isNavigating = false
                            }
                        } catch (e: Exception) {
                            Log.e("Navigation", "Error in onContinue", e)
                            isNavigating = false
                        }
                    }
                },
                onSkipLogin = {
                    if (isNavigating) return@OnboardingScreen
                    isNavigating = true
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToPrivacyPolicy = {
                    navController.navigate(Screen.PrivacyPolicy.route)
                },
                onNavigateToTerms = {
                    navController.navigate(Screen.Terms.route)
                }
            )
        }

        composable(
            route = Screen.ProfileCreation.route
        ) {
            ProfileCreationScreen(
                onNavigateHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.ProfileCreation.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(route = Screen.Home.route) {
            var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

            MainScaffold(
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it },
                onCardClicked = { },
                onNavigateToEmployeeFinder = { companyName ->
                    navController.navigate(Screen.EmployeeFinder.createRoute(companyName))
                },
                onDarkModeToggle = onDarkModeToggle,
                isDarkModeEnabled = isDarkModeEnabled,
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToPrivacyPolicy = {
                    navController.navigate(Screen.PrivacyPolicy.route)
                },
                onNavigateToTerms = {
                    navController.navigate(Screen.Terms.route)
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

        // IntroTemplate route removed — intro flow is now inside EmployeeFinder

        composable(route = Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }

        composable(route = Screen.Terms.route) {
            TermsScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.EmployeeFinder.route,
            arguments = listOf(
                navArgument("companyName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedName = backStackEntry.arguments?.getString("companyName") ?: return@composable
            val decodedName = URLDecoder.decode(encodedName, "UTF-8")

            EmployeeFinderScreen(
                companyName = decodedName,
                onBack = { navController.popBackStack() }
            )
        }
    }
}