# Keep line numbers for crash reporting stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Jellyfin SDK - keep API models used for serialization
-keep class org.jellyfin.sdk.model.api.** { *; }
-keep class org.jellyfin.sdk.model.serializer.** { *; }
-keepclassmembers class org.jellyfin.sdk.model.api.** {
    <init>(...);
}

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.switchstream.**$$serializer { *; }
-keepclassmembers class com.example.switchstream.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.switchstream.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep all @Serializable classes and their serializers
-keepattributes RuntimeVisibleAnnotations
-keep @kotlinx.serialization.Serializable class * { *; }

# Ktor (used by Jellyfin SDK)
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Coil
-dontwarn coil3.**

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Compose - kept by default but just in case
-dontwarn androidx.compose.**
