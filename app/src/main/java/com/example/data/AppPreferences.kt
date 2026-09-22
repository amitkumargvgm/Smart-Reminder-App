package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AppLanguage

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("smart_reminder_prefs", Context.MODE_PRIVATE)

    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var selectedLanguageCode: String
        get() = prefs.getString(KEY_APP_LANGUAGE, AppLanguage.ENGLISH.code) ?: AppLanguage.ENGLISH.code
        set(value) = prefs.edit().putString(KEY_APP_LANGUAGE, value).apply()

    var isPremiumNoAds: Boolean
        get() = prefs.getBoolean(KEY_IS_PREMIUM_NO_ADS, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_PREMIUM_NO_ADS, value).apply()

    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value).apply()

    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_IS_PREMIUM_NO_ADS = "is_premium_no_ads"
        private const val KEY_THEME_MODE = "theme_mode"
    }
}
