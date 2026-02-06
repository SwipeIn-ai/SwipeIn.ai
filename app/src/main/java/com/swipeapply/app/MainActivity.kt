package com.swipeapply.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
    
    companion object {
        private const val TAG = "SwipeApply"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Log app version for debugging cross-device issues
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            Log.i(TAG, "========================================")
            Log.i(TAG, "SwipeApply Starting")
            Log.i(TAG, "Version: ${packageInfo.versionName} (${packageInfo.longVersionCode})")
            Log.i(TAG, "Package: $packageName")
            Log.i(TAG, "========================================")
        } catch (e: Exception) {
            Log.w(TAG, "Could not get package info: ${e.message}")
        }

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