# Keep Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.pis.tvplayer.data.model.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.pis.tvplayer.data.model.**$$serializer { *; }
