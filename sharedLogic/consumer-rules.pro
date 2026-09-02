# Nona Config SDK ProGuard Rules

# Keep the public API
-keep class com.nonaconfig.NonaConfig { *; }
-keep class com.nonaconfig.NonaConfigValue { *; }
-keep class com.nonaconfig.NonaConfigSettings { *; }
-keep class com.nonaconfig.NonaConfigSettings$Builder { *; }

# Keep internal storage keys and logic if they are used via reflection (though we don't currently)
# But it's safer to keep the internal implementations from being over-obfuscated if needed for stability
-keep class com.nonaconfig.internal.** { *; }

# Kotlinx Serialization rules
-keepattributes *Annotation*, EnclosingMethod, Signature
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor rules (usually handled by Ktor itself, but common to include)
-keepattributes RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations, Signature
-keep class io.ktor.** { *; }
