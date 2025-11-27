plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.thatwaz.guesstheemoji"
    compileSdk = 35

    defaultConfig {

        applicationId = "com.thatwaz.guesstheemoji"
        minSdk = 24
        targetSdk = 35
        versionCode = 5
        versionName = "1.0.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

// AndroidX & Compose (MVP core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)

// State/async + prefs
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.datastore.preferences)

// Ads / Billing
    implementation(libs.play.services.ads)
    implementation(libs.billing.ktx)

// (Dev/testing)
    debugImplementation(libs.androidx.ui.tooling)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)

    // ── Optional UI bits ───────────────────────────────────────────
// implementation(libs.androidx.material.icons.extended) // enable if you use extra icons
// implementation(libs.material)                         // only if using Material (Views)

// ── Notifications / background (later) ─────────────────────────
// implementation(libs.androidx.work.runtime.ktx)        // WorkManager (not needed for AlarmManager-based daily reminder)

// ── DI (later, when project grows) ─────────────────────────────
// implementation(libs.hilt.android)
// kapt(libs.hilt.compiler)
// implementation(libs.hilt.navigation.compose)
// implementation(libs.androidx.hilt.common)
// implementation(libs.androidx.hilt.work)

// ── Room (later, for progress/history/catalog) ─────────────────
// implementation(libs.room.runtime)
// implementation(libs.room.ktx)
// kapt(libs.room.compiler)

// ── Networking (if/when you add a backend) ─────────────────────
// implementation(libs.retrofit)
// implementation(libs.retrofit.converter.gson)
// implementation(libs.okhttp.logging)

// ── In-app browser (only if you link out) ──────────────────────
// implementation(libs.androidx.browser)
}