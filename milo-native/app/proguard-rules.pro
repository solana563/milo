-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keep class com.ubayanda.milonative.** { *; }
-keepclassmembers class * { @android.webkit.JavascriptInterface <methods>; }
-keep class com.google.android.gms.cast.** { *; }
-keep class androidx.work.** { *; }
-dontwarn **

# tag editing (jaudiotagger): it reaches some classes by name and mentions desktop-only ones it never uses on Android
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn javax.swing.**
-dontwarn java.beans.**
