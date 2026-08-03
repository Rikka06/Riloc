# Keep Xposed entry
-keep class com.riloc.app.xposed.** { *; }

# libxposed
-keep class io.github.libxposed.** { *; }
-dontwarn io.github.libxposed.**

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.riloc.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.riloc.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
