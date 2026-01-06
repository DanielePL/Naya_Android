import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Load properties from local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

fun getApiKey(property: String): String {
    return localProperties.getProperty(property) ?: ""
}

android {
    namespace = "com.example.menotracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.menotracker"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Supabase Keys
        buildConfigField("String", "SUPABASE_URL", "\"${getApiKey("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${getApiKey("SUPABASE_KEY")}\"")
        buildConfigField("String", "SUPABASE_SERVICE_KEY", "\"${getApiKey("SUPABASE_SERVICE_KEY")}\"")

        // OpenAI API Key (for Nutrition Vision Analysis)
        buildConfigField("String", "OPENAI_API_KEY", "\"${getApiKey("OPENAI_API_KEY")}\"")

        // Anthropic Claude API Key (for AI Coach)
        buildConfigField("String", "ANTHROPIC_API_KEY", "\"${getApiKey("ANTHROPIC_API_KEY")}\"")

        // USDA FoodData Central API Key
        buildConfigField("String", "USDA_API_KEY", "\"${getApiKey("USDA_API_KEY")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // ABI splits for smaller APKs (OpenCV, ONNX Runtime have large native libs)
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = true // Also generate a universal APK
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true // Enable BuildConfig
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

 dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.navigation:navigation-compose:2.9.6")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-video:2.7.0") // Video frame extraction for thumbnails
    implementation("androidx.activity:activity-compose:1.8.2")

    // Supabase
    implementation(platform("io.github.jan-tennert.supabase:bom:2.6.0"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:gotrue-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")
    implementation("io.github.jan-tennert.supabase:functions-kt") // ✨ Edge Functions
    implementation("io.github.jan-tennert.supabase:compose-auth") // Auth UI helpers
    implementation("io.github.jan-tennert.supabase:compose-auth-ui") // Pre-built Auth UI

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Ktor
    implementation("io.ktor:ktor-client-android:2.3.12")
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-client-okhttp:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Kotlinx DateTime
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Room Database
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")

    // OkHttp for VBT API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // CameraX
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // ExifInterface for image rotation
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // ML Kit
    implementation("com.google.mlkit:pose-detection-accurate:18.0.0-beta3")
    implementation("com.google.mlkit:object-detection:17.0.2")
    implementation("com.google.mlkit:object-detection-custom:17.0.2")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("com.google.mlkit:text-recognition:16.0.1") // OCR for nutrition labels
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3") // await() for ML Kit Tasks

    // Accompanist Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // OpenCV for MOSSE/MIL Tracking
    implementation("org.opencv:opencv:4.9.0")

    // Desugaring for java.time API
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Anthropic Claude AI (via HTTP)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Vico Charts (Compose native)
    implementation("com.patrykandpatrick.vico:compose:2.0.0-beta.3")
    implementation("com.patrykandpatrick.vico:compose-m3:2.0.0-beta.3")
    implementation("com.patrykandpatrick.vico:core:2.0.0-beta.3")

     implementation("androidx.media3:media3-exoplayer:1.2.0")
     implementation("androidx.media3:media3-ui:1.2.0")
     implementation("androidx.media3:media3-common:1.2.0")

    // ONNX Runtime for Barbell Detection (YOLO model)
    // Version 1.19.2 required for ONNX IR version 10 (YOLOv11 models with opset 17)
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.19.2")

    // Google Play Billing for In-App Purchases & Subscriptions
    implementation("com.android.billingclient:billing-ktx:7.1.1")

    // Google Play In-App Review API
    implementation("com.google.android.play:review-ktx:2.0.1")

    // Apache POI for Excel file parsing (WOD Scanner)
    implementation("org.apache.poi:poi:5.2.5")
    implementation("org.apache.poi:poi-ooxml:5.2.5") {
        exclude(group = "org.apache.xmlbeans", module = "xmlbeans")
    }
    implementation("org.apache.xmlbeans:xmlbeans:5.2.0") {
        exclude(group = "xml-apis", module = "xml-apis")
    }

    // PdfBox for PDF text extraction (WOD Scanner)
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}