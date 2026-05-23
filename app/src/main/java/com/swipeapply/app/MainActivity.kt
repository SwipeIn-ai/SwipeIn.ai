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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.swipeapply.app.notifications.AppNotificationService
import com.swipeapply.app.notifications.NotificationScheduler
import com.swipeapply.app.ui.navigation.SwipeApplyNavHost
import com.swipeapply.app.ui.theme.SwipeApplyTheme
import io.github.jan.supabase.auth.handleDeeplinks
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "SwipeApply"
        private const val PREFS_NAME = "notification_prefs"
        private const val KEY_ASKED = "notification_asked"
    }

    private val showNotificationRationale = mutableStateOf(false)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Log.i(TAG, "Notification permission granted")
        } else {
            Log.i(TAG, "Notification permission declined — app continues without notifications")
        }
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
            Log.d(TAG, "onCreate intent data=${currentIntent.data}, action=${currentIntent.action}")
            SupabaseClient.client.handleDeeplinks(currentIntent)
        }

        AppNotificationService.ensureChannels(this)
        NotificationScheduler.schedulePeriodicChecks(this)
        requestNotificationPermissionIfNeeded()

        // Initialize contact persistence (loads saved contacts from Room DB)
        com.swipeapply.app.data.repository.SavedContactsRepository.initialize(this)

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

                // Notification permission rationale dialog
                if (showNotificationRationale.value) {
                    AlertDialog(
                        onDismissRequest = {
                            showNotificationRationale.value = false
                            markNotificationAsked()
                        },
                        title = {
                            Text(
                                text = "Stay Updated",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("SwipeIn can notify you about:")
                                Text("  •  New job matches that fit your profile")
                                Text("  •  Follow-up reminders for outreach emails")
                                Text("  •  Updates on saved opportunities")
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "You can change this anytime in your device settings.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showNotificationRationale.value = false
                                    markNotificationAsked()
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(
                                            Manifest.permission.POST_NOTIFICATIONS
                                        )
                                    }
                                }
                            ) { Text("Enable Notifications") }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showNotificationRationale.value = false
                                    markNotificationAsked()
                                }
                            ) { Text("Not Now") }
                        },
                        shape = RoundedCornerShape(24.dp)
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d(TAG, "onNewIntent received. data=${intent.data}, action=${intent.action}")
        SupabaseClient.client.handleDeeplinks(intent)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val alreadyGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (alreadyGranted) return

        // Only ask once — don't nag
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ASKED, false)) return

        // Show rationale dialog before requesting system permission
        showNotificationRationale.value = true
    }

    private fun markNotificationAsked() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ASKED, true)
            .apply()
    }
}