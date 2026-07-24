# Release Notes Template

Ostatnia aktualizacja: 2026-07-14

Użyj tego szablonu dla każdego release candidate, wdrożenia testowego, produkcyjnego i hotfixa.

## Release metadata

```text
Version name:
Version code:
Release date:
Commit / tag:
Build artifact:
Firebase project:
Google Play track:
Release owner:
QA owner:
```

## Release type

- [ ] Internal Testing
- [ ] Closed Testing
- [ ] Open Testing
- [ ] Production
- [ ] Hotfix

## User-visible changes

### New

-

### Improvements

-

### Fixed

-

### Accessibility

-

## Technical changes

### Architecture / data

-

### Firebase / backend

-

### Performance

-

### Security / privacy

-

### Tests and quality

-

## Permissions and Data Safety impact

```text
Manifest permissions changed: yes / no
Runtime permission flow changed: yes / no
Firebase SDK or data collection changed: yes / no
Data Safety update required: yes / no
Privacy Policy update required: yes / no
Account deletion flow changed: yes / no
```

Opis:

-

## Backend and migration order

- [ ] Firestore Rules deployed
- [ ] Storage Rules deployed
- [ ] Indexes deployed
- [ ] Cloud Functions deployed
- [ ] Remote Config reviewed
- [ ] App Check configuration reviewed
- [ ] Data migration completed
- [ ] Compatibility with previous active build verified

Migration notes:

-

Rollback notes:

-

## Known issues and accepted risks

| Issue | Severity | Workaround | Owner | Follow-up |
| --- | --- | --- | --- | --- |
|  |  |  |  |  |

## Validation evidence

- [ ] CI green
- [ ] signed AAB built
- [ ] clean install PASS
- [ ] update from previous version PASS
- [ ] smoke PASS
- [ ] regression PASS
- [ ] runtime permissions matrix PASS
- [ ] account deletion PASS
- [ ] accessibility check PASS
- [ ] Firestore and Storage Rules PASS
- [ ] App Check smoke PASS
- [ ] Crashlytics and Android vitals reviewed
- [ ] Data Safety matches final build
- [ ] public Privacy Policy and account deletion URL verified

Links to evidence:

-

## Rollout plan

```text
Initial percentage:
Next checkpoints:
Monitoring window:
Owner:
Halt criteria:
Rollback / mitigation:
```

- [ ] 5%
- [ ] 20%
- [ ] 50%
- [ ] 100%

Procenty są przykładowe i powinny odpowiadać rzeczywistemu planowi w Google Play Console.

## Monitoring plan

Monitorujemy:

- crash rate i ANR,
- auth,
- Mapę, Listę i uploady,
- runtime permissions,
- account deletion,
- App Check i Rules,
- Firebase/Maps usage i koszty,
- zgłoszenia użytkowników.

## GO / NO-GO

```text
Decision: GO / NO-GO
Date and time:
Decision owner:
Open blockers:
Accepted risks:
Next review:
```

## Related issues and pull requests

-
