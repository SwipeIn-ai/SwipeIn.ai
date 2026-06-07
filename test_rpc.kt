import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest

fun test() {
    val client = createSupabaseClient("url", "key") {
        install(Postgrest)
    }
    client.postgrest.rpc("delete_user")
}
