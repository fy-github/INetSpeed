# ProGuard/R8 rules for INetSpeed

# Keep Hilt generated code
-keepclasseswithmembers class * {
    @dagger.hilt.* <fields>;
}
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep Room entities and DAOs
-keep class com.ikuai.inetspeed.core.data.model.** { *; }
-keep class com.ikuai.inetspeed.core.data.dao.** { *; }
-keep class com.ikuai.inetspeed.core.data.db.** { *; }

# Keep Compose
-dontwarn androidx.compose.**

# Keep Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep iperf3 related classes
-keep class com.ikuai.inetspeed.core.iperf3.** { *; }

# Keep sync module
-keep class com.ikuai.inetspeed.core.sync.** { *; }

# Keep privacy module
-keep class com.ikuai.inetspeed.core.privacy.** { *; }

# Remove debug logging in release
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}

# Keep errorprone annotations (required by Tink/Security-Crypto)
-dontwarn com.google.errorprone.annotations.**
-keep class com.google.errorprone.annotations.** { *; }

# Keep Tink crypto library
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
