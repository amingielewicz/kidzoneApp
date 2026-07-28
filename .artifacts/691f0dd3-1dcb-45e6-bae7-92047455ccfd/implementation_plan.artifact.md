# Implementation Plan - Achievement Dialog Stabilization

Refine the achievement system to group multiple badges earned during the initial sync into a single "Summary" dialog, avoiding multiple sequential pop-ups.

## User Review Required

> [!IMPORTANT]
> - A "Collection Window" will be introduced in the `ProfileViewModel`. During the first ~500ms after entering the Profile screen, all newly detected badges will be buffered.
> - The congratulations dialog will only appear after this buffer period ends or after the primary ranking sync completes.
> - Individual windows will still be used for badges earned *after* the initial summary is dismissed.

## Proposed Changes

### Presentation Layer - Achievements

#### [MODIFY] [ProfileViewModel.kt](file:///C:/Users/Adam/AndroidStudioProjects/playgroundApp/app/src/main/java/com/kidzone/presentation/profile/ProfileViewModel.kt)
- Introduce a `isInitialSync` flag and a `MutableStateFlow` to buffer badges.
- Update `detectNewBadges` to respect the buffer period:
    - If it's the "Initial Sync" phase, add badges to the buffer.
    - Set a small delay (`delay(500)`) in the `init` block before moving buffered badges to the visible `uiState.newlyEarnedBadges`.
- Ensure that `consumeNewlyEarnedBadge` clears the buffer and marks the "Initial Sync" as finished.

#### [MODIFY] [ProfileScreen.kt](file:///C:/Users/Adam/AndroidStudioProjects/playgroundApp/app/src/main/java/com/kidzone/presentation/profile/ProfileScreen.kt)
- No significant changes needed, as it already supports a list of badges. It will now simply receive the full list at once more reliably.

## Verification Plan

### Automated Tests
- Run `ProfileViewModelTest` to verify that badges detected in two different passes (local vs network) are correctly aggregated before the dialog is triggered.

### Manual Verification
1. **The "Welcome Back Summary" Test**:
   - Reinstall app or clear `badge_prefs`.
   - Ensure you have data that triggers multiple badges (e.g. 5 places + rank).
   - Enter Profile.
   - **Expectation**: A single dialog appears showing *all* earned badges together.
2. **The "New Milestone" Test**:
   - Dismiss the summary dialog.
   - Perform an action that earns a *new* badge (if possible via debug tools or mock).
   - **Expectation**: A new, separate dialog appears for just that badge.
