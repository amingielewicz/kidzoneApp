# kidZone documentation

Centralny indeks dokumentacji technicznej, produktowej, QA, bezpieczeństwa i procesu wydawniczego projektu kidZone.

## Start tutaj

- [Główny README projektu](../README.md)
- [Architektura systemu](architecture/ARCHITECTURE.md)
- [Przepływ ekranów Android](android/SCREEN_FLOW.md)
- [Strategia testów](qa/testing-strategy.md)
- [Proces wydania](release/RELEASE_PROCESS.md)
- [Bezpieczeństwo](security.md)

## Architecture

- [Architecture overview](architecture/ARCHITECTURE.md)
- [Clean Architecture](architecture/CLEAN_ARCHITECTURE.md)
- [Modules](architecture/MODULES.md)
- [Dependency rules](architecture/DEPENDENCY_RULES.md)
- [Data flow](architecture/DATA_FLOW.md)
- [State management](architecture/STATE_MANAGEMENT.md)
- [Error handling](architecture/ERROR_HANDLING.md)
- [Observability](architecture/OBSERVABILITY.md)
- [Firebase architecture](architecture/FIREBASE.md)
- [Maps architecture](architecture/MAPS.md)

## Architecture Decision Records

- [ADR-001: Clean Architecture](architecture/ADR/ADR-001-clean-architecture.md)
- [ADR-002: Compose Navigation](architecture/ADR/ADR-002-compose-navigation.md)
- [ADR-003: Firebase](architecture/ADR/ADR-003-firebase.md)
- [ADR-004: StateFlow](architecture/ADR/ADR-004-stateflow.md)
- [ADR-005: Offline strategy](architecture/ADR/ADR-005-offline-strategy.md)

## Android

- [Offline mode](android/OFFLINE_MODE.md)
- [Navigation](android/NAVIGATION.md)
- [Screen flow](android/SCREEN_FLOW.md)
- [ViewModel guide](android/VIEWMODEL_GUIDE.md)
- [Android permissions and Google Play compliance](legal/android-permissions-play-compliance.md)

## Firebase and API

- [Firestore schema](api/FIRESTORE_SCHEMA.md)
- [Firestore collections](api/COLLECTIONS.md)
- [Firestore indexes](api/INDEXES.md)
- [Firestore security rules](api/SECURITY_RULES.md)
- [Storage structure](api/STORAGE_STRUCTURE.md)

## Backend

- [Cloud Functions](backend/CLOUD_FUNCTIONS.md)
- [Backend events](backend/EVENTS.md)
- [Push notifications](backend/PUSH_NOTIFICATIONS.md)

## Development

- [Coding guidelines](development/CODING_GUIDELINES.md)
- [UI/UX guidelines](development/UI_UX_GUIDELINES.md)
- [Performance and security testing](development/PERFORMANCE_SECURITY_TESTING.md)

## Product

- [Design principles](product/DESIGN_PRINCIPLES.md)
- [Product decisions](product/PRODUCT_DECISIONS.md)
- [UI checklist](product/UI_CHECKLIST.md)
- [UX checklist](product/UX_CHECKLIST.md)

## QA and testing

- [Testing strategy](qa/testing-strategy.md)
- [Manual release test plan](qa/manual-release-test-plan.md)
- [Android permissions device matrix](qa/android-permissions-device-matrix.md)
- [Google Play security checklist](qa/google-play-security-checklist.md)
- [Widget privacy checklist](qa/widget-privacy-checklist.md)
- [Test strategy](testing/TEST_STRATEGY.md)
- [Test plan](testing/TEST_PLAN.md)
- [Smoke tests](testing/SMOKE_TESTS.md)
- [Regression testing](testing/REGRESSION.md)

## Release

- [Release overview](release.md)
- [Detailed release process](release/RELEASE_PROCESS.md)
- [Go/No-Go checklist](release/GO_NO_GO_CHECKLIST.md)
- [Hotfix process](release/HOTFIX_PROCESS.md)
- [Play Store release guide](release/PLAY_STORE_RELEASE.md)
- [Release notes template](release/RELEASE_NOTES_TEMPLATE.md)
- [Versioning guide](release/VERSIONING.md)

## Accessibility

- [Accessibility guide](accessibility/ACCESSIBILITY_GUIDE.md)
- [TalkBack guide](accessibility/TALKBACK.md)
- [Large font guide](accessibility/LARGE_FONT.md)

## Operations

- [Monitoring](operations/MONITORING.md)
- [Firebase operations](operations/FIREBASE_OPERATIONS.md)
- [Incident response](operations/INCIDENT_RESPONSE.md)
- [Backups](operations/BACKUPS.md)
- [Firebase cost alerts](firebase-cost-alerts.md)

## Security and compliance

- [Security overview](security.md)
- [Firebase security plan](firebase-security-plan.md)
- [Abuse and rate limiting](abuse-rate-limiting.md)
- [App Check](app-check.md)
- [Android permissions and Google Play compliance](legal/android-permissions-play-compliance.md)
- [Google Play Data Safety draft](legal/google-play-data-safety-draft.md)

## Zasady utrzymania dokumentacji

- Aktualizuj dokument razem ze zmianą kodu, której dotyczy.
- Nie duplikuj instrukcji w kilku plikach. Wstaw odnośnik do dokumentu źródłowego.
- Dodawaj nowe dokumenty do tego indeksu.
- Dla procesów release, bezpieczeństwa i zgodności podawaj datę ostatniej aktualizacji.
- Nazwy klas, ścieżki i komendy zapisuj w backtickach.
