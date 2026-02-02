package com.swipeapply.app

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.ExternalAuthAction
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://wjhmwdctjcymmunmqcft.supabase.co", 
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6IndqaG13ZGN0amN5bW11bm1xY2Z0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjgwMTYxODcsImV4cCI6MjA4MzU5MjE4N30.ehrs7opNY_w79VpP746xfOpLrot0LUUVcPSlbAIhjSU"
    ) {
        install(Auth) {
            scheme = "swipeapply"
            host = "callback"
            defaultExternalAuthAction = ExternalAuthAction.CustomTabs()
        }
        install(Postgrest)
    }
}