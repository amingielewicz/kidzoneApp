# Staging Environment

## Firebase Project Alias

Staging environment uses a separate Firebase project alias (`staging`) to isolate:
- Firestore data
- Cloud Functions
- Hosting (app + admin panel)
- Storage
- Authentication

## Setup

1. **Create a staging Firebase project** in the Firebase Console (e.g., `playground-705e7162-staging`).

2. **Verify alias** is configured in `.firebaserc`:
   ```json
   {
     "projects": {
       "default": "playground-705e7162",
       "staging": "playground-705e7162-staging"
     }
   }
   ```

3. **Set up hosting targets** for the staging project:
   ```bash
   firebase use staging
   firebase target:apply hosting app playground-705e7162-staging
   firebase target:apply hosting admin kidzone-admin-staging
   ```

4. **Deploy to staging**:
   ```bash
   firebase use staging
   firebase deploy
   ```

5. **Deploy only specific services**:
   ```bash
   firebase use staging
   firebase deploy --only firestore:rules
   firebase deploy --only functions
   firebase deploy --only hosting:admin
   ```

## CI/CD Integration

The staging environment can be deployed automatically on PR merge to a `develop` branch
or manually via GitHub Actions workflow dispatch.

### Environment Variables

For CI deployment to staging, set these secrets in GitHub:
- `FIREBASE_SERVICE_ACCOUNT_STAGING` — Service account JSON for `playground-705e7162-staging`

### Admin Panel Staging

The admin panel staging deployment uses `.env.staging`:
```
VITE_FIREBASE_PROJECT_ID=playground-705e7162-staging
VITE_FIREBASE_API_KEY=<staging-api-key>
VITE_FIREBASE_AUTH_DOMAIN=playground-705e7162-staging.firebaseapp.com
```

## Differences from Production

| Aspect | Production | Staging |
|--------|-----------|---------|
| Firebase project | `playground-705e7162` | `playground-705e7162-staging` |
| Remote Config | Production values | Test/preview values |
| Firestore | Live user data | Test data (can be wiped) |
| Auth | Real users | Test accounts |
| Functions | Stable release | Preview deployments |
