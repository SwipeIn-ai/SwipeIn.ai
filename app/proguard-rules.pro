# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ============================================================================
# CRITICAL ATTRIBUTES
# ----------------------------------------------------------------------------
# `proguard-android-optimize.txt` does NOT keep these by default.
# Retrofit needs `Signature` to read the generic / suspend-function return
# types (e.g. `suspend fun getJobs(): FindWorkApiResponse`). Without it, R8
# erases the generic type information and Retrofit throws at runtime in the
# release build, so NO jobs are ever fetched and the UI shows the loading
# skeleton forever — even though the network APIs respond correctly.
# ============================================================================
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# ============================================================================
# APP DATA / MODEL / API CLASSES
# ----------------------------------------------------------------------------
# Keep all data classes, API response models, Retrofit interfaces and the
# generated kotlinx-serialization `$serializer` classes living in the data
# package so Gson (reflective field mapping) and kotlinx.serialization keep
# working after obfuscation.
# ============================================================================
-keep class com.swipeapply.app.data.** { *; }
-keepclassmembers class com.swipeapply.app.data.** { *; }

# Keep Retrofit service interfaces (methods + annotations must survive R8).
-keep interface com.swipeapply.app.data.api.** { *; }

# Some @Serializable response models live outside the data package
# (e.g. ProfileResponse / SupabaseProfileResponse in the viewmodel package).
-keep class com.swipeapply.app.ui.viewmodel.** { *; }
-keepclassmembers class com.swipeapply.app.ui.viewmodel.** { *; }
-keep class com.swipeapply.app.notifications.** { *; }
-keepclassmembers class com.swipeapply.app.notifications.** { *; }

# ============================================================================
# RETROFIT
# ----------------------------------------------------------------------------
# Retrofit 2.9.0 does not ship consumer ProGuard rules, so they must be
# declared here.
# ============================================================================
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Retain generic type information on Retrofit-annotated methods.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# ============================================================================
# OKHTTP / OKIO
# ============================================================================
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ============================================================================
# GSON
# ----------------------------------------------------------------------------
# Gson maps JSON onto fields by name via reflection, so model fields and the
# @SerializedName annotations must be preserved.
# ============================================================================
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers class * {
    @com.google.gson.annotations.Expose <fields>;
}
-dontwarn com.google.gson.**

# ============================================================================
# KOTLINX SERIALIZATION
# ----------------------------------------------------------------------------
# Keep the generated serializers for every @Serializable class regardless of
# the package it lives in (Supabase/Ktor responses, ProfileResponse, etc.).
# ============================================================================
-dontnote kotlinx.serialization.**

-keepclassmembers class **$$serializer {
    *** descriptor;
    *** childSerializers();
    *** typeParametersSerializers();
    <fields>;
}

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class **
-keepclasseswithmembers class <1> {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.swipeapply.app.**$$serializer { *; }
-keepclassmembers class com.swipeapply.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.swipeapply.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ============================================================================
# KOTLIN COROUTINES
# ============================================================================
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-keepclassmembers class kotlin.coroutines.SafeContinuation { volatile <fields>; }
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Kotlin metadata / intrinsics.
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# ============================================================================
# SUPABASE / KTOR
# ============================================================================
-dontwarn io.github.jan.supabase.**
-dontwarn io.ktor.**
-keep class io.github.jan.supabase.** { *; }
-keepclassmembers class io.ktor.** { volatile <fields>; }
-dontwarn org.slf4j.**

# ============================================================================
# COMPOSE
# ============================================================================
-keep class androidx.compose.** { *; }

# ============================================================================
# PDFBOX (resume parsing) — suppress optional/unused dependency warnings
# ============================================================================
-dontwarn com.gemalto.jp2.JP2Decoder
-dontwarn com.tom_roush.pdfbox.**
-keep class com.tom_roush.pdfbox.** { *; }

# ============================================================================
# MISC
# ============================================================================
# Keep enum values (used via name() / valueOf() in swipe + status logic).
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}