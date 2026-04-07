# Block Panic ProGuard Rules

# WebRTC - keep all WebRTC classes
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# Gson - keep serialization classes
-keep class jp.riverapp.blockpanic.engine.** { *; }
-keep class jp.riverapp.blockpanic.network.RoomInfo { *; }
-keep class jp.riverapp.blockpanic.network.SignalInfo { *; }
-keep class jp.riverapp.blockpanic.model.GameRecord { *; }

# Keep enum values for Gson
-keepclassmembers enum * {
    @com.google.gson.annotations.SerializedName <fields>;
}
