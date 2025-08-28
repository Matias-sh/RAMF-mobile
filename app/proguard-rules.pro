# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preservar información de línea para debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Reglas para Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Reglas para Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Mantener clases de modelo de datos
-keep class com.cocido.ramf.data.models.** { *; }
-keep class com.cocido.ramf.data.responses.** { *; }

# Reglas para OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# Reglas para Google Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Reglas para Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# Reglas para AndroidX
-keep class androidx.** { *; }
-dontwarn androidx.**