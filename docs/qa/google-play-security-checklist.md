# Google Play security release checklist

Powiązane issue: #210, #269, #272, #274, #275, #290, #291, #292, #293, #294, #298, #303

Ostatnia aktualizacja: 2026-07-13

## Cel

Finalna bramka jakości przed Release Candidate i publikacją w Google Play.

Statusy:

- `OK` — sprawdzone i gotowe,
- `Do poprawy` — wymaga poprawki albo osobnego issue,
- `Nie dotyczy` — świadomie poza zakresem release.

## 1. Build i podpis

| Punkt | Status | Dowód |
| --- | --- | --- |
| Signed AAB buduje się z aktualnego `main` lub brancha release. | Do poprawy | |
| `versionCode` jest wyższy niż ostatni upload. | Do poprawy | |
| `versionName` odpowiada release notes i tagowi. | Do poprawy | |
| Release używa produkcyjnego `google-services.json`. | Do poprawy | |
| `MAPS_API_KEY` nie jest placeholderem. | Do poprawy | |
| Keystore i hasła nie znajdują się w repo. | Do poprawy | |
| Build release nie zawiera debug providerów ani debug endpointów. | Do poprawy | |

## 2. Automatyczne testy

| Punkt | Status | Dowód |
| --- | --- | --- |
| `detekt` przechodzi. | Do poprawy | |
| `testDebugUnitTest` przechodzi. | Do poprawy | |
| `assembleDebug` przechodzi. | Do poprawy | |
| Signed release build przechodzi. | Do poprawy | |
| Firestore Rules tests przechodzą. | Do poprawy | |
| Gitleaks nie wykrywa sekretów. | Do poprawy | |
| Brak otwartych P0/P1 blockerów. | Do poprawy | |

## 3. Firebase i App Check

| Punkt | Status | Dowód |
| --- | --- | --- |
| Firestore Rules są wdrożone. | Do poprawy | |
| Storage Rules są wdrożone. | Do poprawy | |
| App Check release używa Play Integrity. | Do poprawy | #291 |
| App Check enforcement jest sprawdzony. | Do poprawy | #291 |
| Crashlytics rejestruje kontrolowany test bez danych użytkownika. | Do poprawy | |
| Analytics, Performance, FCM i Remote Config odpowiadają Data Safety. | Do poprawy | #272 |
| Surowe wyjątki i dane użytkownika nie trafiają do breadcrumbs ani custom keys. | Do poprawy | |

## 4. Google Maps i koszty

| Punkt | Status | Dowód |
| --- | --- | --- |
| Klucz Maps jest ograniczony do package name i SHA. | Do poprawy | #293 |
| Billing i alerty kosztowe są skonfigurowane. | Do poprawy | |
| Firebase budget alerts są skonfigurowane. | Do poprawy | |
| Paginacja, cache i limity zapytań są zaakceptowane. | Do poprawy | |
| Smoke test na większym zbiorze danych jest wykonany. | Do poprawy | |

## 5. Prywatność i Google Play

| Punkt | Status | Dowód |
| --- | --- | --- |
| Privacy Policy URL działa po HTTPS. | Do poprawy | #275 |
| Terms URL działa po HTTPS. | Do poprawy | #275 |
| Account deletion URL działa po HTTPS. | Do poprawy | #275 |
| Data Safety jest zapisane w Play Console. | Do poprawy | #272 |
| Data Safety odpowiada aktywnym SDK i finalnemu buildowi. | Do poprawy | #272 |
| Manifest nie zawiera `ACCESS_BACKGROUND_LOCATION`. | Do poprawy | #274 |
| Manifest nie zawiera `READ_MEDIA_IMAGES` ani `READ_EXTERNAL_STORAGE`. | Do poprawy | #274 |
| Photo Picker działa bez szerokiego dostępu do galerii. | Do poprawy | #274, #303 |
| Grupa docelowa to rodzice i opiekunowie, nie dzieci. | Do poprawy | |

## 6. Runtime permissions

| Punkt | Status | Dowód |
| --- | --- | --- |
| Lokalizacja działa dla allow approximate i precise. | Do poprawy | #274 |
| Pierwsza odmowa nie powoduje crasha. | Do poprawy | #303 |
| Kolejna odmowa nie tworzy martwego przycisku. | Do poprawy | #303 |
| Trwała odmowa lokalizacji prowadzi do ustawień aplikacji. | Do poprawy | #303 |
| Kamera działa dla allow i deny. | Do poprawy | #303 |
| Trwała odmowa kamery prowadzi do ustawień aplikacji. | Do poprawy | #303 |
| Powiadomienia są opcjonalne. | Do poprawy | #274 |
| Aplikacja działa bez lokalizacji, kamery i powiadomień. | Do poprawy | #274, #303 |

## 7. Usuwanie konta

| Punkt | Status | Dowód |
| --- | --- | --- |
| Email/password działa end-to-end. | Do poprawy | #210, #292 |
| Logowanie Google działa end-to-end albo ograniczenie jest opisane. | Do poprawy | #292 |
| Auth, Firestore, Storage i FCM są zweryfikowane po usunięciu. | Do poprawy | #210 |
| Publiczne treści są usunięte albo zanonimizowane. | Do poprawy | #210 |
| Lokalny cache nie pokazuje danych po restarcie. | Do poprawy | #210 |

## 8. Widget i cache

| Punkt | Status | Dowód |
| --- | --- | --- |
| Widget nie pokazuje danych po logout. | Do poprawy | #294 |
| Widget nie pokazuje danych po delete account. | Do poprawy | #294 |
| Widget działa bez lokalizacji. | Do poprawy | #294 |
| Cache nie ujawnia prywatnych danych innego konta. | Do poprawy | |

## 9. Manual smoke i accessibility

| Punkt | Status | Dowód |
| --- | --- | --- |
| Manual release test plan jest wykonany. | Do poprawy | `docs/qa/manual-release-test-plan.md` |
| TalkBack checklist jest wykonana albo świadomie odłożona. | Do poprawy | `docs/accessibility/TALKBACK.md` |
| Login, mapa, lista, szczegóły, dodawanie miejsca, opinie, ranking i profil działają. | Do poprawy | |
| Brak internetu i odmowy uprawnień są obsłużone. | Do poprawy | |
| Brak krytycznych crashy. | Do poprawy | |

## GO / NO-GO

### GO

Release może iść dalej, gdy:

- nie ma P0/P1,
- signed AAB jest poprawny,
- Data Safety, Privacy Policy i account deletion są potwierdzone,
- runtime permissions mają PASS,
- App Check, Rules i Maps key są zweryfikowane,
- właściciel zaakceptował znane ryzyka.

### NO-GO

Release blokujemy, gdy:

- nie ma działającego signed AAB,
- Data Safety nie zgadza się z aplikacją,
- usuwanie konta nie działa,
- po odmowie uprawnienia akcje są martwe,
- widget lub cache pokazuje prywatne dane,
- występują krytyczne błędy podstawowych ścieżek.

## Wynik

```text
Release name:
Build / commit:
Track: Internal / Closed / Open / Production

Security checklist: OK / FAIL / BLOCKED
Privacy and Data Safety: OK / FAIL / BLOCKED
Runtime permissions: OK / FAIL / BLOCKED
Account deletion: OK / FAIL / BLOCKED
GO / NO-GO:
Najważniejsze ryzyka:
Dowody:
```
