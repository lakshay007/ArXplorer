import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    kotlin("kapt")
    id("dagger.hilt.android.plugin")
}

val localProperties = Properties().apply {
    load(FileInputStream(rootProject.file("local.properties")))
}

android {
    namespace = "com.lakshay.arxplorer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lakshay.arxplorer"
        minSdk = 23
        targetSdk = 35
        versionCode = 4
        versionName = "1.3"
        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Add your web client ID from google-services.json
        buildConfigField("String", "WEB_CLIENT_ID", "\"1055780860699-9fr31ou33pe1q0vrqi580obv4862l796.apps.googleusercontent.com\"")
        
        // Get Gemini API key from local.properties
        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${localProperties.getProperty("GEMINI_API_KEY", "")}\""
        )

        // Drop MIPS support
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    configurations.all {
        resolutionStrategy {
            force("androidx.core:core-ktx:1.12.0")
            force("androidx.activity:activity-compose:1.8.2")
            // Force AndroidX versions for PDF viewer
            force("androidx.annotation:annotation:1.7.1")
            force("androidx.legacy:legacy-support-v4:1.0.0")
        }
    }

    // Add packaging options for AndroidX
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            pickFirsts += "androidsupportmultidexversion.txt"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-core:1.5.4")
    implementation("androidx.compose.material:material-icons-extended:1.5.4")
    
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.8.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore")
    
    // Google Sign In
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    
    // Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")
    
    // Accompanist for ViewPager and other utilities
    implementation("com.google.accompanist:accompanist-pager:0.32.0")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.32.0")
    implementation("com.google.accompanist:accompanist-swiperefresh:0.32.0")
    
    // DataStore Preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.litert.support.api)

    // Add multidex support for min SDK 19
    implementation("androidx.multidex:multidex:2.0.1")

    // PDF Viewer with specific version
    implementation("com.github.mhiew:android-pdf-viewer:3.2.0-beta.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Add lifecycle-runtime-ktx dependency
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Google AI Gemini
    implementation("com.google.ai.client.generativeai:generativeai:0.1.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Import the BoM for the Firebase platform
    implementation(platform("com.google.firebase:firebase-bom:33.8.0"))

    // Add the dependency for the Vertex AI in Firebase library
    implementation("com.google.firebase:firebase-vertexai")

    // Coil for Compose
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-android-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// Allow references to generated code
kapt {
    correctErrorTypes = true
}