# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.

# Kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.dude555afk.xeno.** {
    *** Companion;
}
-keepclasseswithmembers class com.dude555afk.xeno.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
