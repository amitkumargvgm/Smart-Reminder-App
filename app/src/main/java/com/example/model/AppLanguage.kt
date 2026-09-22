package com.example.model

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val speechLocaleTag: String
) {
    ENGLISH("en", "English", "English", "en-IN"),
    HINDI("hi", "Hindi", "हिंदी", "hi-IN"),
    PUNJABI("pa", "Punjabi", "ਪੰਜਾਬੀ", "pa-IN"),
    ODIA("or", "Odia", "ଓଡ଼ିଆ", "or-IN"),
    GUJARATI("gu", "Gujarati", "ગુજરાતી", "gu-IN"),
    BENGALI("bn", "Bengali", "বাংলা", "bn-IN"),
    TELUGU("te", "Telugu", "తెలుగు", "te-IN"),
    TAMIL("ta", "Tamil", "தமிழ்", "ta-IN");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
        }
    }
}
