# Implementation Plan - Home Screen Data Loading Optimization

Improve the perceived and actual loading speed of the Home (Start) screen by implementing a multi-stage loading strategy (Cache -> Last Known Location -> Fresh GPS Fix).

## User Review Required

> [!IMPORTANT]
> - `HomeViewModel` will no longer wait for a fresh GPS fix before showing data. It will use the system's last known location to show relevant content immediately.
> - `FirestorePlaceRepository` will be updated to use a geo-aware local fallback for nearby queries, ensuring that even if the network is slow or offline, relevant nearby places are shown if they exist in the cache.

## Proposed Changes

### Data Layer

#### [MODIFY] [FirestorePlaceRepository.kt](file:///C:/Users/Adam/AndroidStudioProjects/playgroundApp/app/src/main/java/com/kidzone/data/repository/FirestorePlaceRepository.kt)
- Update `getPlacesNear` fallback logic:
    - Instead of calling `placeDao.getRecentPlaces` (which ignores coordinates), calculate a bounding box (~50km) around the requested center.
    - Call `placeDao.getPlacesInBounds` to retrieve cached places from that area.
    - This provides a much more accurate "Offline Nearby" experience.

### Presentation Layer

#### [MODIFY] [HomeViewModel.kt](file:///C:/Users/Adam/AndroidStudioProjects/playgroundApp/app/src/main/java/com/kidzone/presentation/home/HomeViewModel.kt)
- Refactor `loadLocationBasedPlaces`:
    - **Stage 1 (Immediate)**: Check `locationProvider.getLastKnownLocation()`. If available, immediately call `loadPlacesForLocation` with these coordinates (marking them as potentially stale).
    - **Stage 2 (Background)**: Start the `getCurrentLocation` retry loop as a separate coroutine.
    - **Stage 3 (Refinement)**: If the fresh fix is received and is significantly different (> 500m) from the location used in Stage 1, trigger a new fetch to refine the results.
- This eliminates the 3-9 second delay currently experienced by users waiting for a fresh GPS fix.

#### [MODIFY] [HomeScreen.kt](file:///C:/Users/Adam/AndroidStudioProjects/playgroundApp/app/src/main/java/com/kidzone/presentation/home/HomeScreen.kt)
- Ensure that the `staleLocationAgeMinutes` label is only shown if the location is older than 5 minutes, to avoid unnecessary UI noise for "recent enough" system fixes.

## Verification Plan

### Automated Tests
- Run `HomeViewModelTest` to ensure that data is requested twice (if Stage 1 and Stage 3 locations differ).
- Run `FirestorePlaceRepositoryTest` to verify the new geo-aware fallback.

### Manual Verification
1. **The "Fast Start" Test**:
   - Open app.
   - **Expectation**: Content should appear almost immediately (within < 1s) based on the last known location, even while the "Acquiring location..." indicator might still be active.
2. **The "Offline Nearby" Test**:
   - Cache some places in City A.
   - Go offline and open app near City A.
   - **Expectation**: Nearby places from City A should appear using the Room fallback.
