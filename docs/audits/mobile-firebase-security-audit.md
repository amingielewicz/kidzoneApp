# Mobile and Firebase security audit

Issue: #175
Date: 2026-06-27

## Scope covered in this PR

This PR covers the analytics/logging privacy slice of the broader mobile and Firebase hardening audit.

## Findings

### Fixed: Analytics sent user-entered text

`AnalyticsHelper.logSearch()` sent the raw Firebase Analytics `search_term` parameter.
Search text can contain place names, addresses, personal data, or other user-entered content.

`AnalyticsHelper.logViewPlace()` also sent `place_name`, which is user-generated/place data and
is not needed for aggregate product analytics.

Change:

- `logSearch()` now sends `query_length` and `results_count`, not the raw search text.
- `logViewPlace()` now sends `place_name_length`, not the raw place name.
- Timber debug logs no longer print raw place names, search text, review IDs, or place IDs in these analytics paths.
- `setUserProperty()` no longer prints raw values to Timber.

### Already good in this slice

- Release logging uses `CrashlyticsTree`, not `Timber.DebugTree`.
- `CrashlyticsTree` has tests for email and token redaction.
- Network security config disables cleartext traffic.
- Android backup is disabled in the manifest and data extraction rules exclude backup/transfer.

## Still open for #175

- App Check enforcement must be verified in Firebase Console.
- Firestore and Storage rules require a dedicated rules review pass.
- Cloud Functions authorization checks need a dedicated audit pass beyond helper tests.
- Photo upload metadata/EXIF behavior should be verified end-to-end on real images.
- Deep links should get a focused test/audit pass for unauthorized access.
