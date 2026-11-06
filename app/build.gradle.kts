plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.antimarcipan"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.antimarcipan"
        minSdk = 26
        targetSdk = 34
        // versionCode roste s každým CI buildem (-PversionCode=<run_number>),
        // aby systém aktualizaci bral jako novější verzi.
        versionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 1
        versionName = (project.findProperty("versionName") as String?) ?: "0.1.0"
        vectorDrawables { useSupportLibrary = true }

        // Commit, ze kterého je build (předáno z CI přes -PgitSha=...).
        val gitSha = (project.findProperty("gitSha") as String?) ?: "dev"
        buildConfigField("String", "GIT_SHA", "\"$gitSha\"")
        // Repo, ze kterého se stahují aktualizace.
        buildConfigField("String", "UPDATE_REPO", "\"Jedn0ta/MTG_Antimarcipan\"")
        buildConfigField("String", "UPDATE_TAG", "\"latest-debug\"")
    }

    // Stálý podpisový klíč — všechny buildy jsou podepsané stejně, takže
    // aktualizaci lze nainstalovat přes předchozí verzi (jinak Android odmítne
    // instalaci kvůli neshodě podpisu).
    signingConfigs {
        create("shared") {
            storeFile = file("antimarcipan.keystore")
            storePassword = "antimarcipan"
            keyAlias = "antimarcipan"
            keyPassword = "antimarcipan"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("shared")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("shared")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    // CameraX + ML Kit (card scanning)
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.mlkit.text.recognition)

    // Networking (Scryfall + EDHREC)
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    // Local collection storage
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Card images
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
}
