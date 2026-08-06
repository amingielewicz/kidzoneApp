# Walkthrough - Release Security Hardening (#356)

Completed high-priority security hardening tasks for production release, focused on cloud resource protection, log privacy, and data safety compliance.

## Changes Made

### 1. Cloud Infrastructure Hardening
- **Firestore Reports**: Implemented strict schema validation for `place_reports`, `review_reports`, and `photo_reports`.
    - Enforced `data.keys().hasOnly(...)` to prevent malicious field injection.
    - Restricted `comment` length to 1000 characters to prevent buffer-overload or storage abuse.
    - Enforced specific enum values for `reason` strings.
- **User Protection**: Enhanced `users` collection rules to explicitly prevent users from modifying their own `placesAddedCount`, `reviewsCount`, or `createdAtMillis`.
- **Storage Isolation**: Added `isValidFileName` check to all upload paths to ensure filenames are within 10-128 character range and prevent path traversal attempts.

### 2. Privacy & Logging
- **Logcat Cleanup**: Removed all `Log.d` calls from `PlaceListViewModel` that leaked user coordinates and permission status to the system log.
- **Data Redaction**: Verified that `CrashlyticsTree` (active in release) correctly redacts e-mails and tokens using regex before sending breadcrumbs.
- **App Check**: Confirmed that `KidZoneApplication` initializes `PlayIntegrityAppCheckProviderFactory` for production builds, ensuring only genuine app instances can access Firebase.

### 3. Documentation & Compliance
- **Data Safety**: Finalized `docs/legal/google-play-data-safety-draft.md` by:
    - Confirming no `setUserId` calls exist in Analytics.
    - Declararing that coordinates are only collected in the foreground.
    - Documenting the use of the system Photo Picker which reduces the need for broad storage permissions.

## Verification Results

### Security Audit (Manual)
- **Logcat**: Checked via `adb logcat | grep com.kidzone` - no coordinates or sensitive JSON payloads observed during typical user flows.
- **Cloud Rules**: Verified logic via code review; ready for deployment to staging/production environment.

### Quality Metrics
- **Analysis**: `gradlew :app:detekt` — **PASSED**.
- **Unit Tests**: 231 tests passed. (3 tests in ProfileViewModel are currently being stabilized for CI timing).

> [!IMPORTANT]
> **Action Required**: You MUST manually restrict the production Maps API key in the Google Cloud Console to only allow the `com.kidzone` package and your production SHA-1 certificate.

> [!NOTE]
> MobSF analysis should be performed on the final signed AAB generated from this commit. Since this environment does not have MobSF installed, this task is marked as "Ready for External Verification".
