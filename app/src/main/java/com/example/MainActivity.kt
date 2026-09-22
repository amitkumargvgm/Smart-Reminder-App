package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppPreferences
import com.example.ui.ReminderScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.LocaleHelper
import com.example.viewmodel.ReminderViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val context = try {
            val prefs = AppPreferences(newBase)
            LocaleHelper.setAppLocale(newBase, prefs.selectedLanguageCode)
        } catch (e: Exception) {
            newBase
        }
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ReminderViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()

            // When currentLanguage changes, create a localized configuration & context so stringResource reflects immediately
            androidx.compose.runtime.key(currentLanguage.code) {
                val locale = Locale(currentLanguage.code)
                Locale.setDefault(locale)

                val baseConfig = LocalConfiguration.current
                val newConfig = remember(currentLanguage.code, baseConfig) {
                    android.content.res.Configuration(baseConfig).apply {
                        setLocale(locale)
                    }
                }

                val baseContext = LocalContext.current
                val localizedContext = remember(currentLanguage.code, baseContext) {
                    baseContext.createConfigurationContext(newConfig)
                }

                CompositionLocalProvider(
                    LocalConfiguration provides newConfig,
                    LocalContext provides localizedContext
                ) {
                    MyApplicationTheme(themeMode = themeMode) {
                        ReminderScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}


