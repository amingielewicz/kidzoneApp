// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.firebase.perf) apply false
    alias(libs.plugins.dependencycheck)
}

dependencyCheck {
    val nvdApiKey = System.getenv("NVD_API_KEY").orEmpty()

    formats = listOf("HTML", "JSON", "SARIF")
    failBuildOnCVSS = 9.0f
    failOnError = true
    scanProjects = listOf(":app")
    scanSet.from(
        "admin-panel/package-lock.json",
        "functions/package-lock.json"
    )

    nvd {
        apiKey = nvdApiKey
        delay = if (nvdApiKey.isBlank()) 16_000 else 3_500
    }

    analyzers {
        assemblyEnabled = false
    }
}
