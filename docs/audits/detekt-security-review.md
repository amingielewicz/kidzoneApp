# Detekt security review

Issue: #167  
Audit date: 2026-06-20  
Status: historical audit artifact  
Last documentation review: 2026-07-14

## Purpose

Ten dokument zachowuje wynik audytu Detekt z 20 czerwca 2026. Nie jest bieżącym źródłem prawdy dla architektury, bezpieczeństwa ani zachowania offline.

Aktualne dokumenty źródłowe:

- `docs/security.md`,
- `docs/android/OFFLINE_MODE.md`,
- `docs/architecture/ERROR_HANDLING.md`,
- `docs/development/PERFORMANCE_SECURITY_TESTING.md`.

## Original summary

W momencie audytu `detekt` przechodził z istniejącym baseline. Większość wpisów dotyczyła utrzymywalności Compose/UI, a istotniejsze ryzyka koncentrowały się wokół integralności synchronizacji offline, szerokiej obsługi wyjątków i złożonych ścieżek zapisu.

## Baseline snapshot from audit date

| Finding type | Count | Security relevance at audit date |
| --- | ---: | --- |
| `MagicNumber` | 201 | niska; głównie UI i animacje |
| `FunctionNaming` | 144 | niska; konwencje Compose |
| `LongMethod` | 51 | średnia w auth/save/report flows |
| `MaxLineLength` | 34 | niska |
| `LongParameterList` | 22 | niska–średnia |
| `ReturnCount` | 16 | niska–średnia |
| `TooManyFunctions` | 16 | niska |
| `CyclomaticComplexMethod` | 15 | średnia w krytycznych flow |
| `TooGenericExceptionCaught` | 14 | średnia |
| `UseCheckOrError` | 9 | niska |
| `SwallowedException` | 8 | średnia |
| `ForbiddenComment` | 6 | średnia w obszarze offline sync |

Liczby są historycznym snapshotem i nie powinny być używane jako aktualny wynik bez ponownego uruchomienia Detekt.

## Finding 1: offline sync integrity

### Stan podczas audytu

`SyncWorker` zawierał nieukończone processory operacji add/update/delete. Audyt wskazywał ryzyko usuwania operacji bez wykonania właściwego zapisu.

### Aktualny status dokumentacyjny

Zapisy offline są obecnie traktowane jako gated. Aplikacja nie powinna informować o sukcesie ani oznaczać operacji jako zsynchronizowanej, dopóki replay nie jest kompletny.

Wymagania przed włączeniem replay:

- kompletne processory,
- typed payload parsing,
- idempotency keys,
- ownership checks,
- retry i dead-letter,
- testy z Firebase Emulator,
- jawny status pending/failed w UI,
- cleanup po logout i account deletion.

Szczegóły: `docs/android/OFFLINE_MODE.md`.

## Finding 2: broad exception handling

Obszary wskazane w audycie obejmowały auth, repository, upload, Remote Config, Performance i ViewModele.

Aktualna zasada:

- oczekiwane wyjątki są mapowane jawnie,
- broad catch pozostaje tylko na granicy infrastruktury,
- surowe komunikaty backendu nie trafiają do UI,
- telemetryka jest sanitizowana,
- błąd częściowy nie jest pełnym sukcesem.

Szczegóły: `docs/architecture/ERROR_HANDLING.md`.

## Finding 3: complex save and edit flows

Audyt wskazywał złożoność ścieżek łączących:

- walidację,
- kompresję,
- wykrywanie duplikatów,
- upload,
- Firestore write,
- cleanup,
- zmianę UI state.

Zalecenie pozostaje aktualne: nowe zachowanie powinno być wydzielane do testowalnych współpracowników zamiast dalszego rozbudowywania jednej metody.

## Compose baseline noise

Wpisy takie jak `MagicNumber`, `FunctionNaming` i `LongParameterList` nie mają jednakowego znaczenia. Baseline nie powinien być automatycznie ignorowany, ale priorytet należy nadawać wpisom dotyczącym:

- auth,
- account deletion,
- permissions,
- zapisów i uploadów,
- Rules,
- moderacji,
- offline sync.

## Follow-up references

Historycznie audyt wskazał:

- #252 — offline sync write integrity,
- #253 — typed exception and error mapping.

Status issue należy sprawdzać bezpośrednio w GitHub. Ten dokument nie potwierdza ich bieżącego stanu.

## Running a new review

```powershell
$env:MAPS_API_KEY="AIzaSyPlaceholder"; .\gradlew.bat detekt
```

Nowy audyt powinien zapisać:

```text
Date:
Commit:
Detekt version:
Baseline SHA:
Result:
New findings:
Resolved findings:
Security-relevant changes:
```

## Interpretation

Ten plik jest zapisem historycznym. Każde twierdzenie o aktualnym stanie kodu wymaga nowego uruchomienia narzędzia na bieżącym commicie.
