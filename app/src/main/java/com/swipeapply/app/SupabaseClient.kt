package com.swipeapply.app

import android.util.Base64
import android.util.Log
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.ExternalAuthAction
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.okhttp.OkHttp
import org.json.JSONObject

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL, 
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        // Use OkHttp engine — required for Android devices that enforce
        // hostname-aware checkServerTrusted() when domain-config is present
        // in network_security_config.xml (e.g. Xiaomi/MIUI).
        httpEngine = OkHttp.create()

        install(Auth) {
            scheme = "swipeapply"
            host = "callback"
            sessionManager = SettingsSessionManager(key = "swipeapply.auth.session")
            autoLoadFromStorage = true
            autoSaveToStorage = true
            alwaysAutoRefresh = true
            defaultExternalAuthAction = ExternalAuthAction.CustomTabs()
        }
        install(Postgrest)
    }

    /**
     * Get the current user ID reliably. Tries currentUserOrNull first,
     * then falls back to decoding the JWT access token locally (no network).
     */
    fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id ?: extractUserIdFromJwt()
    }

    private fun extractUserIdFromJwt(): String? {
        return try {
            val token = client.auth.currentSessionOrNull()?.accessToken ?: return null
            val parts = token.split(".")
            if (parts.size != 3) return null
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING))
            JSONObject(payload).optString("sub").takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Failed to decode user ID from JWT", e)
            null
        }
    }
}