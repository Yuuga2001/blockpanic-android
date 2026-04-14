# Block Panic ProGuard Rules

# ============================================================
# WebRTC - keep all WebRTC classes (stream-webrtc-android)
# ============================================================
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**
-keep class io.getstream.webrtc.** { *; }
-dontwarn io.getstream.webrtc.**

# ============================================================
# Gson - keep ALL classes used for JSON serialization
# ============================================================
# Engine types (PlayerState, BlockState, StateMessage, etc.)
-keep class jp.riverapp.blockpanic.engine.** { *; }

# Network types (RoomInfo, SignalInfo, AND inner response classes)
-keep class jp.riverapp.blockpanic.network.** { *; }

# Model types (GameRecord)
-keep class jp.riverapp.blockpanic.model.** { *; }

# Keep enum values and @SerializedName fields
-keepclassmembers enum * { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Gson uses generic type info at runtime via TypeToken
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# ============================================================
# OkHttp
# ============================================================
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okio.** { *; }

# ============================================================
# Kotlin Coroutines
# ============================================================
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { *; }

# ============================================================
# Kotlin Metadata (needed for reflection)
# ============================================================
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keep class kotlin.reflect.** { *; }

# ============================================================
# Game Coordinator & Engine (prevent stripping callbacks)
# ============================================================
-keep class jp.riverapp.blockpanic.game.** { *; }
-keep class jp.riverapp.blockpanic.i18n.** { *; }
-keep class jp.riverapp.blockpanic.rendering.** { *; }
