package com.swipeapply.app

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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
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
}