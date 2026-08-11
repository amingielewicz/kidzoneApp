import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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

// -----------------------------------------------------------------------------
// Secrets
// -----------------------------------------------------------------------------

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")

    if (file.exists()) {
        file.inputStream().use { load(it) }
    }

}

fun resolveSecret(key: String): String =
    localProperties.getProperty(key)
        ?: (project.findProperty(key) as String?)
        ?: System.getenv(key)
        ?: ""

// -----------------------------------------------------------------------------
// Android
// -----------------------------------------------------------------------------

android {
    namespace = "com.kidzone"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kidzone"
        minSdk = 26
        targetSdk = 35

        versionCode = providers.exec {
            commandLine("git", "rev-list", "--count", "HEAD")
        }.standardOutput.asText.get()
            .trim()
            .toIntOrNull()
            ?: 1

        versionName = run {
            val baseVersion = "1.0.0"

            val branch = providers.exec {
                commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
            }.standardOutput.asText.get()
                .trim()

            if (branch == "main" || branch == "master" || branch == "HEAD") {
                baseVersion
            } else {
                val buildIdentifier =
                    providers.environmentVariable("PR_NUMBER").orNull
                        ?: providers.gradleProperty("PR_NUMBER").orNull
                        ?: Regex("\\d+").find(branch)?.value
                        ?: providers.exec {
                            commandLine("git", "rev-parse", "--short=7", "HEAD")
                        }.standardOutput.asText.get()
                            .trim()

                "$baseVersion-dev#$buildIdentifier"
            }
        }

        testInstrumentationRunner = "com.kidzone.HiltTestRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        val mapsApiKey = resolveSecret("MAPS_API_KEY")

        if (mapsApiKey.isBlank()) {
            logger.warn(
                "[kidzone] MAPS_API_KEY is empty. " +
                        "Set MAPS_API_KEY in local.properties."
            )
        }

        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey

        buildConfigField(
            "String",
            "ADMIN_NAME",
            "\"${resolveSecret("ADMIN_NAME")}\""
        )
    }

// -------------------------------------------------------------------------
// Signing
// -------------------------------------------------------------------------

    signingConfigs {
        create("release") {
            val keystoreFile = resolveSecret("KEYSTORE_PATH")
            val keystorePassword = resolveSecret("KEYSTORE_PASSWORD")
            val keyAliasValue = resolveSecret("KEY_ALIAS")
            val keyPasswordValue = resolveSecret("KEY_PASSWORD")

            if (
                keystoreFile.isNotBlank() &&
                keystorePassword.isNotBlank() &&
                keyAliasValue.isNotBlank() &&
                keyPasswordValue.isNotBlank()
            ) {
                storeFile = file(keystoreFile)
                storePassword = keystorePassword
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
            } else {
                logger.warn(
                    "[kidzone] Release signing is not configured. " +
                            "Set KEYSTORE_PATH, KEYSTORE_PASSWORD, KEY_ALIAS " +
                            "and KEY_PASSWORD."
                )
            }
        }
    }

// -------------------------------------------------------------------------
// Build Types
// -------------------------------------------------------------------------

    buildTypes {
        debug {
            manifestPlaceholders[
                "firebasePerformanceInstrumentationEnabled"
            ] = "false"
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

// -------------------------------------------------------------------------
// Java
// -------------------------------------------------------------------------

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

// -------------------------------------------------------------------------
// Compose / BuildConfig
// -------------------------------------------------------------------------

    buildFeatures {
        compose = true
        buildConfig = true
    }

// -------------------------------------------------------------------------
// Packaging
// -------------------------------------------------------------------------

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

// -------------------------------------------------------------------------
// Tests
// -------------------------------------------------------------------------

    @Suppress("UnstableApiUsage")
    testOptions {
        unitTests.isReturnDefaultValues = true

        unitTests.all {
            it.useJUnitPlatform()
        }
    }

}

// -----------------------------------------------------------------------------
// Kotlin
// -----------------------------------------------------------------------------

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

// -----------------------------------------------------------------------------
// Detekt
// -----------------------------------------------------------------------------

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$rootDir/detekt.yml")
    baseline = file("detekt-baseline.xml")
    parallel = true
    ignoreFailures = false
}

// -----------------------------------------------------------------------------
// Dependencies
// -----------------------------------------------------------------------------

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

// Credential Manager / Google Sign-In
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.play.services.auth)

// Google Maps
    implementation(libs.maps.compose)
    implementation(libs.maps.compose.utils)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)

// Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)

// Images
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.androidx.exifinterface)

// Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

// Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

// Google Play
    implementation(libs.play.app.update)
    implementation(libs.play.app.update.ktx)
    implementation(libs.play.review)
    implementation(libs.play.review.ktx)

// Glance
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

// Logging / UI
    implementation(libs.timber)
    implementation(libs.konfetti.compose)

// WorkManager
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.work.compiler)

// Unit tests
    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.arch.core.testing)

// Debug
    debugImplementation(libs.leakcanary)

// Instrumented / UI tests
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.navigation.testing)

    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
}
