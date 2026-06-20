# Accessibility TalkBack checklist

Use this checklist for release candidates and accessibility-focused PRs. Test on a real Android device when possible; emulator checks are useful, but TalkBack timing and focus behavior can differ.

## Setup

- [ ] Turn on TalkBack.
- [ ] Test light mode and dark mode.
- [ ] Test Android font scale at default and at least 1.3x.
- [ ] Test Android display size at default and large.
- [ ] Test with reduced motion enabled.
- [ ] Test with a signed-in regular user and an admin account.

## Android app

### Login and registration

- [ ] Email and password fields announce their labels and required/error states.
- [ ] Password visibility button announces the current action: "Pokaż hasło" or "Ukryj hasło".
- [ ] Login, registration, Google sign-in and password reset actions are reachable by swipe navigation.
- [ ] Permission rationale screens are announced before system permission dialogs.

### Add place

- [ ] Required-field errors are announced after attempting to save an incomplete form.
- [ ] Category and amenities controls announce selected/unselected state.
- [ ] Location, camera and photo picker actions explain why the permission or picker is needed.
- [ ] Photo thumbnails and remove buttons have meaningful labels.
- [ ] The save button state is understandable when the form is incomplete.

### Map

- [ ] Loading and error banners are announced without stealing focus repeatedly.
- [ ] My location, zoom in and zoom out controls have clear accessible names.
- [ ] The map list fallback opens from the map and every item is reachable by swipe navigation.
- [ ] Place cards in the fallback list announce name, category, distance and rating/new state.

### Place details

- [ ] Title, category, address, rating and report actions are announced in a useful order.
- [ ] Review creation and photo reporting flows are reachable without touch exploration.
- [ ] Reward or badge animations respect reduced motion settings.

### Profile

- [ ] Custom profile navigation rows expose button role and at least a 48 dp touch target.
- [ ] Badges, settings and logout actions are reachable and have clear names.
- [ ] Status banners use polite or assertive announcements appropriately.

## Admin panel

- [ ] Admin login page exposes a clear landmark and form labels.
- [ ] Desktop navigation has a labelled nav landmark.
- [ ] Mobile menu button announces open and close state.
- [ ] Tables and action buttons expose descriptive names for users, places, reports and change requests.
- [ ] Dialog close buttons and destructive actions include the affected entity in their accessible name.

## Automated checks

Run these before opening or merging an accessibility PR:

```powershell
$env:MAPS_API_KEY="AIzaSyPlaceholder"; .\gradlew.bat testDebugUnitTest detekt assembleDebug lintDebug assembleDebugAndroidTest
```

```powershell
cd admin-panel
npm.cmd ci
npm.cmd run lint
npm.cmd run format:check
npm.cmd run a11y:check
npm.cmd run build
```

Run device UI tests when a device or emulator is available:

```powershell
$env:MAPS_API_KEY="AIzaSyPlaceholder"; .\gradlew.bat connectedDebugAndroidTest
```
