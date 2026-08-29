package com.klarfinance.app.core.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object OtpVerification : Screen("otp/{phone}") {
        const val ARG_PHONE = "phone"
        fun createRoute(phone: String) = "otp/$phone"
    }
}
