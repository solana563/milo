plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.ubayanda.milonative"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ubayanda.milonative"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // "full" has Google Cast; "fdroid" leaves out every proprietary Google library so F-Droid can build it.
    flavorDimensions += "store"
    productFlavors {
        create("full") {
            dimension = "store"
            signingConfig = signingConfigs.getByName("debug")
        }
        create("fdroid") { dimension = "store" }
    }
    // no signing-info blob in the APK (required for F-Droid reproducible builds)
    dependenciesInfo { includeInApk = false; includeInBundle = false }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-opt-in=androidx.compose.ui.text.ExperimentalTextApi")
    }
    buildFeatures { compose = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.4.1")
    implementation("androidx.media3:media3-session:1.4.1")
    "fullImplementation"("androidx.media3:media3-cast:1.4.1")
    "fullImplementation"("androidx.mediarouter:mediarouter:1.7.0")
    // Google sign-in (Credential Manager). The F-Droid build has no Google libraries, so it gets a stub instead.
    "fullImplementation"("androidx.credentials:credentials:1.3.0")
    "fullImplementation"("androidx.credentials:credentials-play-services-auth:1.3.0")
    "fullImplementation"("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("androidx.media3:media3-common:1.4.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
