# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.example.antimarcipan.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
