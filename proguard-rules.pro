# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ============================================================================
# KOTLINX SERIALIZATION
# ============================================================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
}
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keepclasseswithmembers class ** {
    @kotlinx.serialization.Serializable <fields>;
}

# Keep @Serializable classes
-keep,includedescriptorclasses class com.example.myapplicationtest.**$$serializer { *; }
-keepclassmembers class com.example.myapplicationtest.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.myapplicationtest.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ============================================================================
# KTOR
# ============================================================================
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { volatile <fields>; }
-keep class kotlin.reflect.jvm.internal.** { *; }
-dontwarn io.ktor.**

# ============================================================================
# SUPABASE
# ============================================================================
-keep class io.github.jan.supabase.** { *; }
-keepclassmembers class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**

# ============================================================================
# RETROFIT & GSON
# ============================================================================
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**

-keep class com.google.gson.** { *; }
-keepattributes *Annotation*
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ============================================================================
# OKHTTP
# ============================================================================
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ============================================================================
# ONNX RUNTIME
# ============================================================================
-keep class ai.onnxruntime.** { *; }
-keepclassmembers class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**

# ============================================================================
# OPENCV
# ============================================================================
-keep class org.opencv.** { *; }
-keepclassmembers class org.opencv.** { *; }
-dontwarn org.opencv.**

# ============================================================================
# ML KIT
# ============================================================================
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ============================================================================
# GOOGLE PLAY SERVICES & CREDENTIALS
# ============================================================================
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-keep class androidx.credentials.** { *; }

# ============================================================================
# ROOM DATABASE
# ============================================================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ============================================================================
# COMPOSE
# ============================================================================
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ============================================================================
# COIL
# ============================================================================
-keep class coil.** { *; }
-dontwarn coil.**

# ============================================================================
# VICO CHARTS
# ============================================================================
-keep class com.patrykandpatrick.vico.** { *; }
-dontwarn com.patrykandpatrick.vico.**

# ============================================================================
# APACHE POI (Excel parsing) - Complex library with many optional dependencies
# ============================================================================
-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.openxmlformats.**

# POI optional dependencies (not available on Android)
-dontwarn aQute.bnd.annotation.spi.ServiceConsumer
-dontwarn aQute.bnd.annotation.spi.ServiceProvider
-dontwarn java.awt.**
-dontwarn javax.swing.**
-dontwarn org.osgi.framework.**
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn com.graphbuilder.**

# Log4j (used by POI)
-dontwarn org.apache.logging.log4j.**
-keep class org.apache.logging.log4j.** { *; }

# ============================================================================
# PDFBOX
# ============================================================================
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**

# ============================================================================
# KOTLIN COROUTINES
# ============================================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ============================================================================
# GENERAL KOTLIN
# ============================================================================
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# ============================================================================
# APP MODELS (keep all data classes for serialization)
# ============================================================================
-keep class com.example.myapplicationtest.data.** { *; }
-keep class com.example.myapplicationtest.onboarding.data.** { *; }
-keep class com.example.myapplicationtest.community.data.** { *; }
-keep class com.example.myapplicationtest.screens.vbt.** { *; }