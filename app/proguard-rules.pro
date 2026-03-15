# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep data classes for serialization
-keep class com.swipeapply.app.data.model.** { *; }

# Keep Compose classes
-keep class androidx.compose.** { *; }
