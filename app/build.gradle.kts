import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.firebase.perf)
    alias(libs.plugins.detekt)
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
        versionCode = providers.exec {
            commandLine("git", "rev-list", "--count", "HEAD")
        }.standardOutput.asText.get().trim().toIntOrNull() ?: 1
        versionName = run {
            val baseVersion = "1.0.0"
            // Na branchach dev/feature dodajemy suffix dev#<numerPR>.
            // Np. branch po merge jako PR #63 → "0.1.0-dev#63".
            // Na main (release) zostaje czyste "0.1.0".
            //
            // Łańcuch rozwiązywania numeru:
            //  1. Zmienna środowiskowa PR_NUMBER (ustawiana w CI/CD)
            //  2. Gradle property -PPR_NUMBER=72 (lokalne override)
            //  3. Cyfry wyciągnięte z nazwy brancha (np. fix/72-opis → "72")
            //  4. Skrócony commit hash (7 znaków) jako ostateczny fallback
            val branch = providers.exec {
                commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
            }.standardOutput.asText.get().trim()
            if (branch == "main" || branch == "master" || branch == "HEAD") {
                baseVersion
            } else {
                val prNumber = System.getenv("PR_NUMBER")
                    ?: (project.findProperty("PR_NUMBER") as String?)
                    ?: Regex("\\d+").find(branch)?.value
                    ?: providers.exec {
                        commandLine("git", "rev-parse", "--short=7", "HEAD")
                    }.standardOutput.asText.get().trim()
                "$baseVersion-dev#$prNumber"
            }
        }

        testInstrumentationRunner = "com.kidzone.HiltTestRunner"
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

    /**
     * Release signing config – ładowany z tych samych źródeł co MAPS_API_KEY:
     *   local.properties (dev) → gradle.properties / -P → zmienne środowiskowe (CI/CD).
     *
     * Wymagane zmienne:
     *   KEYSTORE_PATH             — ścieżka do keystore (.jks / .keystore)
     *   KEYSTORE_PASSWORD         — hasło do keystore
     *   KEY_ALIAS                 — alias klucza
     *   KEY_PASSWORD              — hasło do klucza
     *
     * Jeśli brakuje któregokolwiek — release build zostanie niepodpisany
     * (build przejdzie, ale AAB/APK nie zainstaluje się na urządzeniu).
     */
    signingConfigs {
        create("release") {
            val keystoreFile = resolveSecret("KEYSTORE_PATH")
            val keystorePass = resolveSecret("KEYSTORE_PASSWORD")
            val keyAlias = resolveSecret("KEY_ALIAS")
            val keyPass = resolveSecret("KEY_PASSWORD")

            if (keystoreFile.isNotBlank() && keystorePass.isNotBlank() &&
                keyAlias.isNotBlank() && keyPass.isNotBlank()
            ) {
                storeFile = file(keystoreFile)
                storePassword = keystorePass
                this.keyAlias = keyAlias
                keyPassword = keyPass
            } else {
                logger.warn(
                    "[kidzone] Release signing not configured. Set KEYSTORE_PATH, " +
                        "KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD " +
                        "in local.properties or environment."
                )
            }
        }
    }

    buildTypes {
        debug {
            // Disable Firebase Performance in debug/CI builds to prevent crashes
            // when google-services.json contains a placeholder API key.
            manifestPlaceholders["firebasePerformanceInstrumentationEnabled"] = "false"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
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

    @Suppress("UnstableApiUsage")
    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$rootDir/detekt.yml")
    baseline = file("detekt-baseline.xml")
    parallel = true
    ignoreFailures = false
}

dependencies {
    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)

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
    implementation(libs.firebase.functions.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)
    implementation(libs.firebase.perf)
    implementation(libs.firebase.config.ktx)

    // Credential Manager (Google Sign-In przez nowoczesne API)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Google Maps
    implementation(libs.maps.compose)
    implementation(libs.maps.compose.utils)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)

    // Legacy Google Sign-In (fallback for devices where Credential Manager fails)
    implementation(libs.play.services.auth)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)

    // Image loading
    implementation(libs.coil.compose)

    // ExifInterface (auto-rotate photos before upload)
    implementation(libs.androidx.exifinterface)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // In-App Update
    implementation(libs.play.app.update)
    implementation(libs.play.app.update.ktx)

    // In-App Review
    implementation(libs.play.review)
    implementation(libs.play.review.ktx)

    // Glance AppWidget
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // Logging
    implementation(libs.timber)

    // Konfetti
    implementation(libs.konfetti.compose)

    // WorkManager (offline sync queue)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.work.compiler)

    // ===== Testing =====
    // JUnit 5
    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testRuntimeOnly(libs.junit5.engine)

    // MockK
    testImplementation(libs.mockk)

    // Coroutines Test
    testImplementation(libs.kotlinx.coroutines.test)

    // Turbine (Flow testing)
    testImplementation(libs.turbine)

    // AndroidX Arch Core (InstantTaskExecutorRule equivalent)
    testImplementation(libs.androidx.arch.core.testing)

    // LeakCanary (debug only — memory leak detection)
    debugImplementation(libs.leakcanary)

    // ===== Instrumented / UI Tests (androidTest) =====
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.navigation:navigation-testing:2.8.2")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.52")
    kspAndroidTest("com.google.dagger:hilt-android-compiler:2.52")
}
