# Implementation Plan - Resolve Gradle Deprecations

Address Gradle deprecation warnings to ensure compatibility with Gradle 9.0 and improve build performance (Configuration Cache support).

## User Review Required

> [!IMPORTANT]
> - The logic for `versionCode` and `versionName` will be refactored to use `providers.exec` without immediate `.get()` calls where possible, or by following current Gradle 8.x best practices for value sourcing.
> - The `packaging` block will be updated to use modern collection DSL.

## Proposed Changes

### Build Configuration

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/Adam/AndroidStudioProjects/playgroundApp/app/build.gradle.kts)
- Refactor `versionCode` and `versionName` resolution:
    - Use `providers.exec { ... }.standardOutput.asText.map { it.trim() }` to stay lazy where possible.
    - Note: AGP's `versionCode` and `versionName` DSL currently requires non-provider values. I will ensure the retrieval is done cleanly.
- Update `packaging` DSL:
    - Change `excludes += ...` to `excludes.add(...)` or `resources.excludes.add(...)` if applicable for modern Gradle `SetProperty`.
- Move `it.useJUnitPlatform()` check if needed (though it's usually fine).

### Project Properties

#### [MODIFY] [gradle.properties](file:///C:/Users/Adam/AndroidStudioProjects/playgroundApp/gradle.properties)
- Add `org.gradle.configuration-cache=true` to proactively catch issues that lead to Gradle 9.0 incompatibilities.
- Ensure `android.nonTransitiveRClass=true` is present (already is).

## Verification Plan

### Automated Tests
- Run `gradlew help --warning-mode all` and verify that the number of project-related warnings has decreased.
- Run `gradlew assembleDebug` to ensure the build still produces valid APKs with correct versioning.
- Run unit tests: `gradlew :app:testDebugUnitTest`.

### Manual Verification
- Check the generated `BuildConfig` or APK details to confirm `versionCode` and `versionName` are still correctly extracted from Git.
