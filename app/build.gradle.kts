import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

/**
 * Sekrety dla developera (klucze API itp.) trzymamy w `local.properties`,
 * bo to plik gitignorowany z natury (Android Studio sam go tak traktuje).
 *
 * Niestety `project.findProperty()` Gradle'a czyta tylko `gradle.properties`
 * – `local.properties` jest specjalny i jest parsowany jedynie przez Android
 * Gradle Plugin do wyciągnięcia `sdk.dir`. Dlatego ładujemy go tu ręcznie,
 * żeby manifestPlaceholders mogły z niego korzystać.
 *
 * Łańcuch: local.properties -> gradle.properties / -P -> zmienna środowiskowa.
 */
val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun resolveSecret(key: String): String =
    localProperties.getProperty(key)
        ?: (project.findProperty(key) as String?)
        ?: System.getenv(key)
        ?: ""

android {
    namespace = "com.kidzone"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kidzone"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // Klucz Google Maps – ładowany przez resolveSecret() z chain:
        //   local.properties (preferowane, gitignored)
        //     -> gradle.properties / -PMAPS_API_KEY=...
        //     -> zmienna środowiskowa MAPS_API_KEY (CI/CD)
        // Pusty klucz = mapa się odpali ale Maps SDK rzuci `Authorization
        // failure` w Logcat i zobaczysz puste szare/zielone tło. Wypisujemy
        // ostrzeżenie w czasie konfiguracji żeby ten przypadek był widoczny.
        val mapsApiKey = resolveSecret("MAPS_API_KEY")
        if (mapsApiKey.isBlank()) {
            logger.warn(
                "[kidzone] MAPS_API_KEY is empty. Set it in local.properties " +
                    "(MAPS_API_KEY=AIza...) – mapa nie będzie się renderowała."
            )
        }
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)

    // Credential Manager (Google Sign-In przez nowoczesne API)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Google Maps
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)

    // Image loading
    implementation(libs.coil.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
}
