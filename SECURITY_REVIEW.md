# Security Review

## Current security baseline

Implemented:

- HTTPS only (`cleartextTrafficPermitted=false`)
- `allowBackup=false`
- FileProvider configured
- Firebase messaging service not exported
- Google Maps API key injected from build configuration
- Gitleaks secret scanning in CI

## Manual review checklist

Before every release:

- [ ] Verify no API keys are committed.
- [ ] Verify Firebase configuration is correct.
- [ ] Verify deep links validate input parameters.
- [ ] Verify exported components are necessary.
- [ ] Verify permissions are requested only when needed.
- [ ] Verify release build has no debug-only configuration.
- [ ] Verify Android CI is green.

## Areas requiring future review

- Deep link validation (`placeId`)
- App Widget receiver attack surface
- Firebase Firestore security rules
- Firebase Storage security rules
- Authentication and authorization flows
- Release build hardening
