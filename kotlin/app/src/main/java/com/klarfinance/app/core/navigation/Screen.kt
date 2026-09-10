package com.klarfinance.app.core.navigation

import com.klarfinance.app.domain.model.AccountState

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Home : Screen("home?accountState={accountState}") {
        const val ARG_ACCOUNT_STATE = "accountState"
        fun createRoute(accountState: AccountState = AccountState.GUEST) = "home?accountState=${accountState.name}"
    }
    data object Login : Screen("login")
    data object Account : Screen("account")
    data object Referral : Screen("referral")

    /** Nested graph root - History list dan Payment berbagi satu HistoryViewModel (mirror
     * RequestLoanGraph), supaya Payment gak perlu network call baru buat data cicilan yang
     * sudah kebawa dari GET /loan/history. */
    data object HistoryGraph : Screen("history")
    data object History : Screen("history/list")
    data object Payment : Screen("history/payment/{loanId}") {
        const val ARG_LOAN_ID = "loanId"
        fun createRoute(loanId: Int) = "history/payment/$loanId"
    }

    /** Nested graph root, mirip RegisterGraph - berbagi satu RequestLoanViewModel lintas 2
     * langkah (nominal+tenor, lalu rekening tujuan+submit). */
    data object RequestLoanGraph : Screen("request-loan")
    data object LoanAmount : Screen("request-loan/amount")
    data object LoanBankAccount : Screen("request-loan/bank-account")

    /** Bayar QRIS - beda dari RequestLoanGraph, cuma satu data (merchantCode, hasil decode QR)
     * yang perlu dibawa ke layar berikutnya, cukup lewat nav arg biasa, gak perlu ViewModel
     * di-share lintas 2 layar. */
    data object QrisScan : Screen("qris/scan")
    data object QrisAmount : Screen("qris/amount/{merchantCode}") {
        const val ARG_MERCHANT_CODE = "merchantCode"
        fun createRoute(merchantCode: String) = "qris/amount/$merchantCode"
    }

    /** Nested graph root - Home (beli qty tiket + riwayat used/belum) dan Confirm (bayar)
     * berbagi satu TransjakartaPurchaseViewModel, mirror RequestLoanGraph. Gak ada
     * halte/rute/maps (GTFS+Maps dibuang, konfirmasi user 2026-09-10 sesi lanjutan) - tiket
     * generik, QR ditampilin inline di riwayat (TransjakartaHomeScreen), bukan halaman terpisah. */
    data object TransjakartaGraph : Screen("transjakarta")
    data object TransjakartaHome : Screen("transjakarta/home")
    data object TransjakartaConfirm : Screen("transjakarta/confirm")

    data object OtpVerification : Screen("otp/{phone}") {
        const val ARG_PHONE = "phone"
        fun createRoute(phone: String) = "otp/$phone"
    }
    data object PasswordLogin : Screen("password-login/{phone}") {
        const val ARG_PHONE = "phone"
        fun createRoute(phone: String) = "password-login/$phone"
    }

    /** Nested graph root. Holds the shared [com.klarfinance.app.presentation.register.RegisterViewModel]. */
    data object RegisterGraph : Screen("register/{phone}") {
        const val ARG_PHONE = "phone"
        fun createRoute(phone: String) = "register/$phone"
    }
    data object KtpScan : Screen("register/ktp-scan")
    data object SelfieCapture : Screen("register/selfie-capture")
    data object VerifyIdentity : Screen("register/verify-identity")
    data object CompleteProfile : Screen("register/complete-profile")
    data object RegisterSuccess : Screen("register/success")
}
