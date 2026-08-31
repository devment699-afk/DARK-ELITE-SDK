# DARK SDK ProGuard Rules
# Keep public API classes
-keep public class com.dark.sdk.DarkSdk { *; }
-keep public class com.dark.sdk.api.** { *; }
-keep public class com.dark.sdk.BuildConfig { *; }

# Keep annotations
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations

# OkHttp / Okio
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Kotlin Coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Kotlinx Serialization
-keep class kotlinx.serialization.** { *; }
-keep class kotlinx.serialization.json.** { *; }
-dontwarn kotlinx.serialization.**

# AndroidX
-keep class androidx.** { *; }
-dontwarn androidx.**

# Google AutoService (annotation processor)
-keep class com.google.auto.service.** { *; }

# JavaPoet
-keep class com.squareup.javapoet.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Optimize
-optimizationpasses 5
-allowaccessmodification
-mergeinterfacesaggressively

# Obfuscation options for internal classes
-keep class com.dark.sdk.internal.** { *; }
-keep class com.dark.sdk.utils.** { *; }

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}