plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val verCode: Int = (project.findProperty("verCode") as String?)?.toIntOrNull() ?: 1

android {
    namespace = "com.belge.goruntuleyici"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.belge.goruntuleyici"
        minSdk = 26
        targetSdk = 34
        versionCode = verCode
        versionName = "1.$verCode"
    }

    signingConfigs {
        create("release") {
            storeFile = file("belge-key.jks")
            storePassword = "5UJFOumaFgQepKqjRkZI"
            keyAlias = "belge"
            keyPassword = "5UJFOumaFgQepKqjRkZI"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
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
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    implementation("io.github.beyka:Android-TiffBitmapFactory:0.9.9.1")
}
