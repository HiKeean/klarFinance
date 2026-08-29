package com.klarfinance.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.klarfinance.app.presentation.login.LoginScreen
import com.klarfinance.app.presentation.otp.OtpVerificationScreen
import com.klarfinance.app.presentation.splash.SplashScreen

@Composable
fun KlarNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onTimeout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onOtpRequested = { phone ->
                    navController.navigate(Screen.OtpVerification.createRoute(phone))
                },
            )
        }

        composable(
            route = Screen.OtpVerification.route,
            arguments = listOf(navArgument(Screen.OtpVerification.ARG_PHONE) { type = NavType.StringType }),
        ) {
            OtpVerificationScreen(
                onBackClick = { navController.popBackStack() },
            )
        }
    }
}
