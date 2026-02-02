package com.swipeapply.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.swipeapply.app.ui.navigation.SwipeApplyNavHost
import com.swipeapply.app.ui.theme.BackgroundLight
import com.swipeapply.app.ui.theme.BackgroundDark
import com.swipeapply.app.ui.theme.SwipeApplyTheme
import com.swipeapply.app.SupabaseClient
import kotlinx.coroutines.delay
import io.github.jan.supabase.auth.handleDeeplinks
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val currentIntent = intent
        if (currentIntent != null) {
            SupabaseClient.client.handleDeeplinks(currentIntent)
        }
        PDFBoxResourceLoader.init(applicationContext)
        setContent {
            val darkModeEnabled = rememberSaveable { mutableStateOf<Boolean?>(null) }

            val isDarkTheme = when (darkModeEnabled.value) {
                true -> true
                false -> false
                null -> isSystemInDarkTheme()
            }

            SwipeApplyTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (isDarkTheme) BackgroundDark else BackgroundLight
                ) {
                    SwipeApplyNavHost(
                        onDarkModeToggle = { isDarkMode ->
                            darkModeEnabled.value = isDarkMode
                        },
                        isDarkModeEnabled = darkModeEnabled.value
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        SupabaseClient.client.handleDeeplinks(intent)
    }
}