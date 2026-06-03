# ProGuard rules for INetSpeed

# Keep Hilt generated code
-keepclasseswithmembers class * {
    @dagger.hilt.* <fields>;
}

# Keep Room entities
-keep class com.ikuai.inetspeed.core.data.** { *; }

# Keep Compose
-dontwarn androidx.compose.**
