package com.swipeapply.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Terminal


/**
 * Tech stack icon mapping for common technologies.
 * Maps technology names to Material Icons and brand colors.
 */
object TechStackIcons {
    
    data class TechIcon(
        val icon: ImageVector,
        val color: Color
    )
    
    private val iconMap = mapOf(
        // Android
        "kotlin" to TechIcon(Icons.Default.Build, Color(0xFF7F52FF)),
        "jetpack compose" to TechIcon(Icons.Default.Build, Color(0xFF4285F4)),
        "compose" to TechIcon(Icons.Default.Build, Color(0xFF4285F4)),
        "android" to TechIcon(Icons.Default.Build, Color(0xFF3DDC84)),
        "coroutines" to TechIcon(Icons.Default.Settings, Color(0xFF7F52FF)),
        "dagger hilt" to TechIcon(Icons.Default.Lock, Color(0xFFE74C3C)),
        "room" to TechIcon(Icons.Default.Star, Color(0xFF4285F4)),
        "sqldelight" to TechIcon(Icons.Default.Star, Color(0xFF00D2B8)),
        
        // iOS
        "swift" to TechIcon(Icons.Default.Favorite, Color(0xFFFA7343)),
        "swiftui" to TechIcon(Icons.Default.Favorite, Color(0xFF0066CC)),
        
        // Backend
        "node.js" to TechIcon(Icons.Default.CloudQueue, Color(0xFF68A063)),
        "typescript" to TechIcon(Icons.Default.Terminal, Color(0xFF3178C6)),
        "python" to TechIcon(Icons.Default.Terminal, Color(0xFF3776AB)),
        "java" to TechIcon(Icons.Default.Terminal, Color(0xFFED8B00)),
        "go" to TechIcon(Icons.Default.Terminal, Color(0xFF00ADD8)),
        "rust" to TechIcon(Icons.Default.Terminal, Color(0xFFCE412B)),
        
        // Frontend
        "react" to TechIcon(Icons.Default.Build, Color(0xFF61DAFB)),
        "react native" to TechIcon(Icons.Default.Phone, Color(0xFF61DAFB)),
        "vue" to TechIcon(Icons.Default.Build, Color(0xFF42B883)),
        "angular" to TechIcon(Icons.Default.Build, Color(0xFFDD0031)),
        "next.js" to TechIcon(Icons.Default.Build, Color(0xFF000000)),
        "flutter" to TechIcon(Icons.Default.Phone, Color(0xFF02569B)),
        
        // Database
        "postgresql" to TechIcon(Icons.Default.Star, Color(0xFF336791)),
        "mongodb" to TechIcon(Icons.Default.Star, Color(0xFF47A248)),
        "redis" to TechIcon(Icons.Default.Star, Color(0xFFDC382D)),
        "mysql" to TechIcon(Icons.Default.Star, Color(0xFF4479A1)),
        
        // Cloud & Infrastructure
        "aws" to TechIcon(Icons.Default.CloudQueue, Color(0xFFFF9900)),
        "gcp" to TechIcon(Icons.Default.CloudQueue, Color(0xFF4285F4)),
        "azure" to TechIcon(Icons.Default.CloudQueue, Color(0xFF0078D4)),
        "docker" to TechIcon(Icons.Default.Settings, Color(0xFF2496ED)),
        "kubernetes" to TechIcon(Icons.Default.Settings, Color(0xFF326CE5)),
        
        // Tools
        "graphql" to TechIcon(Icons.Default.Star, Color(0xFFE10098)),
        "rest" to TechIcon(Icons.Default.Star, Color(0xFF00AA9E)),
        "grpc" to TechIcon(Icons.Default.Share, Color(0xFF244C5A)),
        "websocket" to TechIcon(Icons.Default.Share, Color(0xFF010101)),
        "firebase" to TechIcon(Icons.Default.Star, Color(0xFFFFCA28)),
        "supabase" to TechIcon(Icons.Default.Star, Color(0xFF3ECF8E)),
        
        // Testing
        "jest" to TechIcon(Icons.Default.Check, Color(0xFFC21325)),
        "cypress" to TechIcon(Icons.Default.Check, Color(0xFF17202C)),
        "junit" to TechIcon(Icons.Default.Check, Color(0xFF25A162)),
        
        // Other
        "git" to TechIcon(Icons.Default.Star, Color(0xFFF05032)),
        "github" to TechIcon(Icons.Default.Star, Color(0xFF181717)),
        "gitlab" to TechIcon(Icons.Default.Star, Color(0xFFFC6D26)),
        "ktor" to TechIcon(Icons.Default.Build, Color(0xFF087CFA)),
        "edge functions" to TechIcon(Icons.Default.CloudQueue, Color(0xFF000000)),
        "webassembly" to TechIcon(Icons.Default.Build, Color(0xFF654FF0)),
        "c++" to TechIcon(Icons.Default.Terminal, Color(0xFF00599C)),
        "plaid sdk" to TechIcon(Icons.Default.Star, Color(0xFF000000)),
        "kotlin multiplatform" to TechIcon(Icons.Default.Build, Color(0xFF7F52FF)),
        "realtime" to TechIcon(Icons.Default.Refresh, Color(0xFF3ECF8E))
    )
    
    /**
     * Get icon for a technology name (case-insensitive)
     */
    fun getIcon(techName: String): TechIcon {
        val key = techName.lowercase()
        return iconMap[key] ?: TechIcon(
            icon = Icons.Default.Build,
            color = Color(0xFF6B7280) // Default gray
        )
    }
}
