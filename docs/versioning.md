# Versioning

The app version is managed in the root-level `version.properties` file.

```properties
VERSION_NAME=1.0.0
VERSION_CODE=1
```

## VERSION_NAME

`VERSION_NAME` is the human-readable app version shown to users.

Use semantic versioning:

```text
MAJOR.MINOR.PATCH
```

Examples:

```text
1.0.0
1.0.1
1.1.0
2.0.0
```

### When to bump PATCH

Increase PATCH for small fixes that do not introduce new user-facing features.

Example:

```text
1.0.0 -> 1.0.1
```

Use PATCH for:

- bug fixes,
- small UI corrections,
- copy/text fixes,
- minor stability improvements.

### When to bump MINOR

Increase MINOR for new user-facing features that remain backward-compatible.

Example:

```text
1.0.1 -> 1.1.0
```

Use MINOR for:

- new screens,
- new filters,
- new place details features,
- new notification behavior,
- new settings.

### When to bump MAJOR

Increase MAJOR for large changes that significantly change app behavior, data model, or release expectations.

Example:

```text
1.1.0 -> 2.0.0
```

Use MAJOR for:

- breaking data migrations,
- authentication model changes,
- major Firebase structure changes,
- complete redesigns,
- incompatible release changes.

## VERSION_CODE

`VERSION_CODE` is the internal Android build number.

It must always increase for every version published to Google Play.

Initial version:

```properties
VERSION_NAME=1.0.0
VERSION_CODE=1
```

Next patch release:

```properties
VERSION_NAME=1.0.1
VERSION_CODE=2
```

Next minor release:

```properties
VERSION_NAME=1.1.0
VERSION_CODE=3
```

## Release rule

Before preparing a release:

1. Update `VERSION_NAME` if the user-facing version changes.
2. Always increase `VERSION_CODE`.
3. Commit the version change.
4. Run Android CI.
5. Run the manual APK build workflow.
6. Only then prepare a signed release build.

## Important

Never store secrets in `version.properties`.

This file should stay committed to the repository.
