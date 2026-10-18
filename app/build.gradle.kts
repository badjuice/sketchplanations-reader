plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "org.rdcl.nox.sketchplanationsreader"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.rdcl.nox.sketchplanationsreader"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "0.3.0"
    }

    // Release signing comes from ~/.gradle/gradle.properties, never from the repository:
    // SKETCH_RELEASE_STORE, SKETCH_RELEASE_PASSWORD, SKETCH_RELEASE_ALIAS.
    val releaseStore = providers.gradleProperty("SKETCH_RELEASE_STORE").orNull
    signingConfigs {
        if (releaseStore != null) create("release") {
            storeFile = file(releaseStore)
            storePassword = providers.gradleProperty("SKETCH_RELEASE_PASSWORD").get()
            keyAlias = providers.gradleProperty("SKETCH_RELEASE_ALIAS").get()
            keyPassword = providers.gradleProperty("SKETCH_RELEASE_PASSWORD").get()
        }
    }

    // One APK per CPU architecture, plus a universal one that runs anywhere.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
            isUniversalApk = true
        }
    }

    buildTypes {
        debug {
            // Test builds name the exact commit: 0.2.0-3-g1a2b3c4, plus -dirty for uncommitted changes.
            versionNameSuffix = gitPosition()
        }
        release {
            // Without the private release key (e.g. on another machine), fall back to the debug key so builds still install.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    implementation("org.rdcl.nox:sketchplanations-explode")

    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.browser:browser:1.8.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-gif:2.7.0")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
}

/** Commits since the last release tag, e.g. "-3-g1a2b3c4". Empty on a tagged commit or without git. */
fun gitPosition(): String = runCatching {
    providers.exec {
        commandLine("git", "describe", "--tags", "--dirty", "--always", "--match", "v[0-9]*")
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim()
}.getOrDefault("").replace(Regex("^v[0-9][^-]*"), "").let { if (it.isEmpty() || it.startsWith("-")) it else "-g$it" }
