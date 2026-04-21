package com.swipeapply.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.swipeapply.app.notifications.AppNotificationService
import com.swipeapply.app.notifications.NotificationScheduler
import com.swipeapply.app.ui.navigation.SwipeApplyNavHost
import com.swipeapply.app.ui.theme.SwipeApplyTheme
import io.github.jan.supabase.auth.handleDeeplinks
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "SwipeApply"
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            Log.i(TAG, "========================================")
            Log.i(TAG, "SwipeApply Starting")
            Log.i(TAG, "Version: ${packageInfo.versionName} ($versionCode)")
            Log.i(TAG, "Package: $packageName")
            Log.i(TAG, "========================================")
        } catch (e: Exception) {
            Log.w(TAG, "Could not get package info: ${e.message}")
        }

        val currentIntent = intent
        if (currentIntent != null) {
            SupabaseClient.client.handleDeeplinks(currentIntent)
        }

        AppNotificationService.ensureChannels(this)
        NotificationScheduler.schedulePeriodicChecks(this)
        requestNotificationPermissionIfNeeded()

        PDFBoxResourceLoader.init(applicationContext)
        setContent {
            val darkModeEnabled = rememberSaveable { mutableStateOf<Boolean?>(false) }

            val isDarkTheme = when (darkModeEnabled.value) {
                true -> true
                false -> false
                null -> false
            }

            SwipeApplyTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
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

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val alreadyGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!alreadyGranted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        }
    }
}
