import java.util.Properties
import java.io.FileInputStream
import java.io.File

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
  id("kotlin-parcelize")
}

android {
    namespace = "com.mobile.superiorchat"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.mobile.superiorchat"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    flavorDimensions += "camouflage"

    productFlavors {
        create("original") {
            dimension = "camouflage"
            applicationId = "com.mobile.superiorchat"
        }
        create("captivePortal") {
            dimension = "camouflage"
            applicationId = "com.android.connectivity.stats"
        }
    }

    sourceSets {
        getByName("captivePortal") {
            java.srcDirs("src/decoyEngine/java")
            kotlin.srcDirs("src/decoyEngine/java")
            res.srcDirs("src/decoyEngine/res")
            manifest.srcFile("src/decoyEngine/AndroidManifest.xml")
        }
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties()
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))
                storeFile = File(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}


dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation("androidx.compose.material:material-icons-extended")
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Telegram Bot Utility Dependencies
  implementation("androidx.security:security-crypto:1.1.0-alpha06")
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

  // Retrofit
  implementation("com.squareup.retrofit2:retrofit:2.11.0")
  implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")

  // Room
  implementation("androidx.room:room-runtime:2.6.1")
  implementation("androidx.room:room-ktx:2.6.1")
  ksp("androidx.room:room-compiler:2.6.1")

  // WorkManager
  implementation("androidx.work:work-runtime-ktx:2.9.0")

  // Coil
  implementation("io.coil-kt:coil-compose:2.6.0")
  implementation("io.coil-kt:coil-video:2.6.0")

  // ZXing for QR Code generation & decoding
  implementation("com.google.zxing:core:3.5.3")

  // CameraX for QR Scanning
  val cameraxVersion = "1.3.3"
  implementation("androidx.camera:camera-camera2:$cameraxVersion")
  implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
  implementation("androidx.camera:camera-view:$cameraxVersion")
}

ksp {
    arg("room.generateKotlin", "true")
}