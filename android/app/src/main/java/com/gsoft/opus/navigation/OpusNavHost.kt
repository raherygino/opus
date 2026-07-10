package com.gsoft.opus.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.gsoft.opus.presentation.login.LoginScreen
import com.gsoft.opus.presentation.main.MainScreen
import com.gsoft.opus.presentation.splash.SplashScreen

@Composable
fun OpusNavHost(
    navController: NavHostController,
    startDestination: String = Routes.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            fadeIn(animationSpec = tween(400)) + scaleIn(
                animationSpec = tween(400),
                initialScale = 0.92f
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300)) + scaleOut(
                animationSpec = tween(300),
                targetScale = 1.05f
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(400)) + scaleIn(
                animationSpec = tween(400),
                initialScale = 1.05f
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) + scaleOut(
                animationSpec = tween(300),
                targetScale = 0.92f
            )
        }
    ) {
        composable(
            route = Routes.Splash.route,
            enterTransition = { fadeIn(tween(500)) },
            exitTransition = { fadeOut(tween(500)) }
        ) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Routes.Login.route) {
                        popUpTo(Routes.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Routes.Main.route) {
                        popUpTo(Routes.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.Login.route,
            enterTransition = {
                slideInHorizontally(tween(400)) { it } + fadeIn(tween(400))
            },
            exitTransition = {
                slideOutHorizontally(tween(300)) { -it / 3 } + fadeOut(tween(300))
            }
        ) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.Main.route) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.Main.route,
            enterTransition = {
                fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 0.95f)
            },
            exitTransition = {
                fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.95f)
            }
        ) {
            MainScreen(
                onLogout = {
                    navController.navigate(Routes.Login.route) {
                        popUpTo(Routes.Main.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
