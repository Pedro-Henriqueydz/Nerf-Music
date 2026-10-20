# Proguard rules for NERF Music
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.media3.common.util.UnstableApi *;
}

## Rules for NewPipeExtractor & Mozilla Rhino JS Engine
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.tools.**
-keep class org.schabi.newpipe.extractor.** { *; }
-keep class com.nerf.music.player.OkHttpDownloader { *; }
-keep class * implements org.schabi.newpipe.extractor.downloader.Downloader { *; }
