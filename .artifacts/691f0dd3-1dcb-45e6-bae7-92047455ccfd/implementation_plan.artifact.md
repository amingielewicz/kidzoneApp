# Implementation Plan - Detekt Issues Cleanup

Address code quality issues identified by Detekt across several files, including complexity, line length, and unused members.

## User Review Required

> [!IMPORTANT]
> - `LoginViewModel` will have `@Suppress("TooManyFunctions")` added as the auth flow requires several specific interaction methods that are better kept explicit.
> - Spread operators in `UiText` and ViewModels will be suppressed as they are the standard way to pass arguments to string resources.
> - The unused `configureFirebaseEmulators` in `KidZoneApplication.kt` will be removed.

## Proposed Changes

### Data Layer

#### [MODIFY] [FirebaseAuthRepository.kt](file:///C:/Users/Adam/AndroidStudioProjects/playgroundApp/app/src/main/java/com/kidzone/data/repository/FirebaseAuthRepository.kt)
- Split `checkBanStatus` by extracting `mapBanReasonCode` and `createBanException` helpers to reduce cyclomatic complexity.

### Presentation Layer

#### [MODIFY] [LoginViewModel.kt](file:///C:/Users/Adam/AndroidStudioProjects/playgroundApp/app/src/main/java/com/kidzone/presentation/auth/LoginViewModel.kt)
- Suppress `TooManyFunctions` as the 11 methods are all distinct UI actions.
- Suppress `SpreadOperator` for the `mapError` function.

#### [MODIFY] [RegisterViewModel.kt](file:///C:/Users/Adam/AndroidStudioProjects/playgroundApp/app/src/main/java/com/kidzone/presentation/auth/RegisterViewModel.kt)
- Suppress `SpreadOperator` for the `mapError` function.

#### [MODIFY] [ProfileViewModel.kt](file:///C:/Users/Adam/AndroidStudioProjects/playgroundApp/app/src/main/java/com/kidzone/presentation/profile/ProfileViewModel.kt)
- Suppress `SpreadOperator` for the `toAuthUiText` function.

#### [MODIFY] [LoginScreen.kt](file:///C:/Users/Adam/AndroidStudioProjects/playgroundApp/app/src/main/java/com/kidzone/presentation/auth/LoginScreen.kt)
- Wrap long lines in the Google Sign-In error handling block.

#### [MODIFY] [PlaceDetailsScreen.kt](file:///C:/Users/Adam/AndroidStudioProjects/playgroundApp/app/src/main/java/com/kidzone/presentation/place/details/PlaceDetailsScreen.kt)
- Wrap the long line in the `formatDistance` function.

### Utils and Core

#### [MODIFY] [FirebaseErrorMapper.kt](file:///C:/Users/Adam/AndroidStudioProjects/playgroundApp/app/src/main/java/com/kidzone/utils/FirebaseErrorMapper.kt)
- Refactor `toPlacesErrorMessage` using a `when` block to reduce return count to 1.
- Suppress `SpreadOperator` for argument passing.

#### [MODIFY] [KidZoneApplication.kt](file:///C:/Users/Adam/AndroidStudioProjects/playgroundApp/app/src/main/java/com/kidzone/KidZoneApplication.kt)
- Remove the unused `configureFirebaseEmulators` function.

### Tests

#### [MODIFY] [ResourceLocalizationTest.kt](file:///C:/Users/Adam/AndroidStudioProjects/playgroundApp/app/src/test/java/com/kidzone/i18n/ResourceLocalizationTest.kt)
- Refactor `verify all resources are localized and consistent` by extracting logic into smaller private methods.
- Use `check()` instead of `throw IllegalStateException`.
- Wrap long lines in error messages.

## Verification Plan

### Automated Tests
- Run Detekt to ensure all issues are resolved: `gradlew detekt`.
- Run unit tests to ensure no regressions: `gradlew :app:testDebugUnitTest`.
