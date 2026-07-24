import java.util.Properties
import java.io.FileInputStream
import java.io.File

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.mobile.superiorsetup"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.mobile.superiorsetup"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "v1.0.0"
    }

    flavorDimensions += "camouflage"

    productFlavors {
        create("original") {
            dimension = "camouflage"
            buildConfigField("String", "TARGET_APP_ID", "\"com.mobile.superiorchat\"")
            manifestPlaceholders["targetAppId"] = "com.mobile.superiorchat"
        }
        create("captivePortal") {
            dimension = "camouflage"
            buildConfigField("String", "TARGET_APP_ID", "\"com.android.connectivity.stats\"")
            manifestPlaceholders["targetAppId"] = "com.android.connectivity.stats"
        }
        create("weather") {
            dimension = "camouflage"
            buildConfigField("String", "TARGET_APP_ID", "\"com.android.weather.info\"")
            manifestPlaceholders["targetAppId"] = "com.android.weather.info"
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
      buildConfig = true
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

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.lifecycle.runtime.compose)
  
  // ZXing for QR Code generation
  implementation("com.google.zxing:core:3.5.3")

  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation("androidx.compose.material:material-icons-extended")
  implementation("androidx.security:security-crypto:1.1.0-alpha06")
  
  // CameraX for QR Scanning
  val cameraxVersion = "1.3.3"
  implementation("androidx.camera:camera-camera2:$cameraxVersion")
  implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
  implementation("androidx.camera:camera-view:$cameraxVersion")
}
