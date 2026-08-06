# Implementation Plan - Release Security Hardening (#356)

Prepare the application for production by hardening cloud rules, cleaning up sensitive logs, and verifying data safety declarations.

## User Review Required

> [!WARNING]
> - **Firestore Rules**: Hardening rules for reports might break older clients if they send unexpected fields. Since we are before V1, this is acceptable.
> - **Logcat**: All `Log.d/i/v` calls in production code will be removed or converted to `Timber` (which is already configured to redact data in release).
> - **Manual Action Required**: I cannot restrict API keys in Google Cloud Console. You **must** do this manually following the checklist in the issue description.

## Proposed Changes

### 1. Data Layer - Security Hardening
#### [MODIFY] [firestore.rules](file:///C:/Users/Adam/AndroidStudioProjects/playgroundApp/firestore.rules)
- Update `isValidPlaceReport`, `isValidReviewReport`, and `isValidPhotoReport`:
    - Add `data.keys().hasOnly(...)` to prevent extra fields.
    - Enforce max length for comments (e.g., 500 characters).
    - Ensure `reason` matches a specific enum set of strings.
- Refine `users` update rules to ensure users cannot increment their own `placesAddedCount` or `reviewsCount`.

#### [MODIFY] [storage.rules](file:///C:/Users/Adam/AndroidStudioProjects/playgroundApp/storage.rules)
- Add size limits to all upload paths.
- Ensure public read access is explicit only for content that is intended to be public (places/reviews).

### 2. Presentation Layer - Privacy & Logging
#### [MODIFY] [PlaceListViewModel.kt](file:///C:/Users/Adam/AndroidStudioProjects/playgroundApp/app/src/main/java/com/kidzone/presentation/place/list/PlaceListViewModel.kt)
- Remove all `Log.d` calls related to user location. These should not be present in release builds.

#### [MODIFY] [ProfileViewModel.kt](file:///C:/Users/Adam/AndroidStudioProjects/playgroundApp/app/src/main/java/com/kidzone/presentation/profile/ProfileViewModel.kt)
- Fix the `profileState` stream structure to resolve failing unit tests while maintaining the "no-flicker" fix.

### 3. Documentation
#### [MODIFY] [google-play-data-safety-draft.md](file:///C:/Users/Adam/AndroidStudioProjects/playgroundApp/docs/legal/google-play-data-safety-draft.md)
- Reconcile declared data types with actual app behavior (e.g., confirming we don't collect "precise location" in background).

## Verification Plan

### Automated Tests
- Run `./gradlew testDebugUnitTest` to ensure no regressions in Profile/Auth logic.
- Run Firestore emulator tests: `firebase emulators:exec --only firestore "npx vitest"`.

### Manual Verification
- **Logcat check**: Run the app in Debug mode, perform location actions, and ensure no sensitive coordinates are logged to standard output.
- **Reporting check**: Try reporting a place with a very long comment to verify rule enforcement.
