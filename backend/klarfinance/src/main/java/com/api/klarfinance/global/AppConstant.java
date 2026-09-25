package com.api.klarfinance.global;

public class AppConstant {
    private AppConstant(){}
    public static final String CACHE_INTERNAL= "internal";
    public static final String CACHE_NASABAH = "nasabah";

    public static final String CACHE_INTERNAL_LOGIN = "internal_login";
    public static final String CACHE_NASABAH_LOGIN = "nasabah_login";

    public static final String OTP_KEY_PREFIX = "otp:";
    public static final String OTP_VERIFIED_KEY_PREFIX = "otp:verified:";
    public static final String OTP_COOLDOWN_KEY_PREFIX = "otp:cooldown:";
    public static final String OTP_HOURLY_COUNT_KEY_PREFIX = "otp:count:";
    public static final String OTP_IP_COUNT_KEY_PREFIX = "otp:ip:";
    public static final String OTP_ATTEMPT_KEY_PREFIX = "otp:attempt:";

    /** Channel OTP yang dikembalikan request-otp - FIREBASE_SMS artinya WhatsApp gagal dan app
     * harus kirim OTP sendiri lewat Firebase Phone Auth, lalu panggil verify-firebase-phone. */
    public static final String OTP_CHANNEL_WHATSAPP = "WHATSAPP";
    public static final String OTP_CHANNEL_FIREBASE_SMS = "FIREBASE_SMS";

    public static final String ROLE_NASABAH = "NASABAH";
}
