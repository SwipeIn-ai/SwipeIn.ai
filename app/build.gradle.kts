import java.util.Properties
import java.io.FileInputStream
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization") // NEW: Add this to enable serialization support
    id("com.google.devtools.ksp") version "2.3.0"
}
val envProperties = Properties()
val envFile = rootProject.file(".env")
if (envFile.exists()) {
    envProperties.load(FileInputStream(envFile))
}
android {
    namespace = "com.swipeapply.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.swipeapply.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.2"

        val geminiKey = envProperties.getProperty("GEMINI_API_KEY") ?: ""
        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"$geminiKey\""
        )
        
        // OpenRouter API Key for AI resume parsing
        val openRouterKey = envProperties.getProperty("OPENROUTER_API_KEY") ?: ""
        buildConfigField(
            "String",
            "OPENROUTER_API_KEY",
            "\"$openRouterKey\""
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true   
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(
                org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
            )
        }
    }
}


dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")
    
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.1")
    
    // ViewModel Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    
    // Accompanist (for system UI controller)
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.36.0")
    
    // Shimmer effect for loading states
    implementation("com.valentinilk.shimmer:compose-shimmer:1.3.0")

    implementation("androidx.compose.material:material-icons-extended")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.03"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Supabase for auth
    implementation(platform("io.github.jan-tennert.supabase:bom:3.3.0"))
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:postgrest-kt") // Optional: For fetching user profiles if needed
    implementation("io.ktor:ktor-client-cio:3.0.0")
    implementation("io.ktor:ktor-client-core:3.0.0")
    implementation("io.ktor:ktor-client-okhttp:3.0.0")
    implementation("io.ktor:ktor-client-content-negotiation:3.0.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.0")
    implementation("androidx.browser:browser:1.9.0")
    
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Room Database
    val room_version = "2.7.0-alpha11" 
    
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")
    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // PDFBox for PDF text extraction
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    
    // Kotlin Serialization (for parsing AI JSON response)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    
    // Note: OpenRouter uses OkHttp (already included above) for HTTP calls
}