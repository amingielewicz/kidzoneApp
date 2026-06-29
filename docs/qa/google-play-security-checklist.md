# Google Play security release checklist

Powiązane issue: #290, #291, #292, #293, #294, #298

Ten dokument jest finalną bramką jakości przed Release Candidate i publikacją w Google Play. Każdy punkt powinien mieć jeden ze statusów:

- `OK` - sprawdzone i gotowe,
- `Do poprawy` - wymaga osobnego issue albo poprawki przed releasem,
- `Nie dotyczy` - świadomie poza zakresem danego release.

Nie zamyka to ręcznych gate'ów w Firebase Console, Google Play Console ani testu usuwania konta. Te wyniki trzeba nadal wpisać w odpowiednich issue.

## Poziomy gotowości

| Poziom | Przeznaczenie | Decyzja |
| --- | --- | --- |
| MVP / Release Candidate | Internal Testing albo Closed Testing | Dopuszczalne tylko przy braku P0/P1 i kompletnych punktach minimum. |
| Optimum / Produkt publiczny | Pierwszy publiczny release | Rekomendowany poziom dla publikacji produkcyjnej. |
| Enterprise / Maximum | Dojrzały produkt zespołowy | Poza zakresem pierwszego release, chyba że zostanie jawnie wymagany. |

## 1. Build i podpis

| Punkt | Status | Dowód / link |
| --- | --- | --- |
| Signed Release Build albo AAB buduje się z aktualnego `main` / brancha release. | Do poprawy | |
| `versionCode` jest większy niż ostatni build wysłany do Google Play. | Do poprawy | |
| `versionName` odpowiada tagowi albo release notes. | Do poprawy | |
| Release używa prawdziwego `google-services.json`. | Do poprawy | |
| Release nie używa placeholderowego `MAPS_API_KEY`. | Do poprawy | |
| Keystore i hasła są w sekrecie/lokalnej konfiguracji, nie w repo. | Do poprawy | |

## 2. Automatyczne testy i jakość

| Punkt | Status | Dowód / link |
| --- | --- | --- |
| `detekt` przechodzi. | Do poprawy | |
| `testDebugUnitTest` przechodzi. | Do poprawy | |
| `assembleDebug` przechodzi. | Do poprawy | |
| Firebase rules tests przechodzą, jeśli są uruchamiane lokalnie. | Do poprawy | |
| Dependency Check ma raport przejrzany albo blokery przeniesione do issue. | Do poprawy | |
| Brak otwartych P0/P1 blockerów release. | Do poprawy | |

## 3. Firebase i App Check

| Punkt | Status | Dowód / link |
| --- | --- | --- |
| Firestore Rules są wdrożone w docelowym projekcie Firebase. | Do poprawy | |
| Storage Rules są wdrożone w docelowym projekcie Firebase. | Do poprawy | |
| App Check release używa Play Integrity. | Do poprawy | |
| App Check enforcement jest sprawdzony w Firebase Console. | Do poprawy | #291 |
| Debug provider nie jest wymagany do działania release builda. | Do poprawy | |
| Crashlytics zbiera testowy crash albo decyzja o odłożeniu jest zapisana. | Do poprawy | |
| Analytics, Performance, FCM i Remote Config są zgodne z Data Safety. | Do poprawy | |

## 4. Google Maps i koszty

| Punkt | Status | Dowód / link |
| --- | --- | --- |
| Google Maps API key jest ograniczony do package name i SHA. | Do poprawy | #293 |
| Google Maps usage i billing alerts są sprawdzone. | Do poprawy | |
| Firebase budget alerts są sprawdzone. | Do poprawy | |
| Cache, paginacja i limity zapytań są zaakceptowane dla RC. | Do poprawy | |
| Smoke test na większym zbiorze miejsc jest wykonany albo świadomie odłożony. | Do poprawy | |

## 5. Prywatność i Google Play

| Punkt | Status | Dowód / link |
| --- | --- | --- |
| Privacy Policy URL działa po HTTPS. | Do poprawy | #275 |
| Terms URL działa po HTTPS, jeśli jest używany w Store Listing. | Do poprawy | #275 |
| Account deletion URL działa po HTTPS. | Do poprawy | #275 |
| Google Play Data Safety jest przepisane i zapisane w Play Console. | Do poprawy | #272 |
| Data Safety odpowiada faktycznym usługom Firebase/Google. | Do poprawy | #272 |
| Uprawnienia Androida w Play Console odpowiadają manifestowi. | Do poprawy | #274 |
| Nie deklarujemy background location, jeśli aplikacja jej nie używa. | Do poprawy | #274 |

## 6. Account deletion

| Punkt | Status | Dowód / link |
| --- | --- | --- |
| Usuwanie konta email/password działa end-to-end. | Do poprawy | #210, #292, #299 |
| Usuwanie konta Google działa end-to-end albo ograniczenie jest opisane. | Do poprawy | #292, #299 |
| Firebase Auth, Firestore, Storage i FCM tokeny są zweryfikowane po usunięciu konta. | Do poprawy | #210 |
| Publiczne treści po usunięciu konta są usunięte albo zanonimizowane zgodnie z polityką. | Do poprawy | #210 |
| Wynik testu jest zapisany w issue albo raporcie QA. | Do poprawy | #210 |

## 7. Widget privacy

| Punkt | Status | Dowód / link |
| --- | --- | --- |
| Widget nie pokazuje prywatnych danych po logout. | Do poprawy | #294 |
| Widget nie pokazuje prywatnych danych po delete account. | Do poprawy | #294 |
| Widget działa bez lokalizacji i bez cache. | Do poprawy | #294 |
| Widget nie odświeża się nadmiernie po broadcastach. | Do poprawy | #294 |
| Treść widgetu na ekranie blokady jest zaakceptowana dla pierwszego release. | Do poprawy | #294 |

## 8. Manual smoke i accessibility

| Punkt | Status | Dowód / link |
| --- | --- | --- |
| Manual release test plan jest wykonany dla podstawowych ścieżek. | Do poprawy | `docs/qa/manual-release-test-plan.md` |
| TalkBack checklist jest wykonana albo świadomie odłożona. | Do poprawy | `docs/qa/accessibility-talkback-checklist.md` |
| Login, rejestracja, mapa, lista, szczegóły, dodawanie miejsca, opinie, ranking i profil działają. | Do poprawy | |
| Brak internetu, odmowa lokalizacji i odmowa powiadomień są obsłużone. | Do poprawy | |
| Nie ma krytycznych crashy w smoke teście. | Do poprawy | |

## Go / No-Go

### GO

Release może iść dalej, gdy:

- wszystkie punkty MVP / Release Candidate mają status `OK` albo świadome `Nie dotyczy`,
- nie ma otwartych P0/P1,
- Data Safety, Privacy Policy i Account deletion są potwierdzone,
- App Check, Firebase Rules, Maps key i release build są sprawdzone,
- właściciel projektu zaakceptował znane ryzyka.

### NO-GO

Release blokujemy, gdy:

- nie ma działającego signed release builda,
- App Check / Firebase Rules / Storage Rules nie są zweryfikowane,
- Data Safety nie zgadza się z realnym działaniem aplikacji,
- usuwanie konta nie przeszło testu end-to-end,
- widget albo cache pokazuje prywatne dane po logout/delete,
- istnieją krytyczne błędy podstawowych ścieżek użytkownika.

## Wynik końcowy

```text
Release name:
Build / commit:
Track: Internal / Closed / Open / Production

MVP / RC: OK / FAIL / BLOCKED
Optimum / public release: OK / FAIL / BLOCKED
Enterprise / maximum: Nie dotyczy / Do poprawy

GO / NO-GO:
Decyzja właściciela:
Najważniejsze ryzyka:
Linki do dowodów:
```
