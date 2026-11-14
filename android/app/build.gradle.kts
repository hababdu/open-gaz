plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Backend manzili. Emulyatorda kompyuter localhost'i 10.0.2.2 bo'ladi.
// O'zgartirish: ./gradlew assembleDebug -PAPI_BASE_URL=https://sizning-server.uz/
val apiBaseUrl: String = (project.findProperty("API_BASE_URL") as String?)?.takeIf { it.isNotBlank() }
    ?: "http://10.0.2.2:8000/"

android {
    namespace = "uz.oppengaz"
    compileSdk = 34

    defaultConfig {
        applicationId = "uz.oppengaz"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.2.0"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Tarmoq
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Xarita: OpenStreetMap (API kalit kerak emas)
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // Joylashuv (API kalit kerak emas)
    implementation("com.google.android.gms:play-services-location:21.3.0")

    testImplementation("junit:junit:4.13.2")
    // JVM testlarida org.json haqiqiy bo'lishi uchun (Android stub emas)
    testImplementation("org.json:json:20240303")
}
