# Testing strategy and regression coverage

Powiązane issue: #176

Ostatnia aktualizacja: 2026-07-13

## Cel

Strategia testów kidZone obejmuje aplikację Android, Firebase Rules, Cloud Functions i panel administracyjny. Dokument określa, co testujemy automatycznie, co manualnie oraz jakie bramki obowiązują przed PR-em i releasem.

## Poziomy testów

| Poziom | Zakres | Narzędzia | Kiedy wymagany |
| --- | --- | --- | --- |
| Unit tests | ViewModel, use case, mappery, walidacje, sortowanie, limity, retry | JUnit, MockK, kotlinx-coroutines-test | każda zmiana logiki |
| Rules tests | Firestore i Storage Rules, ownership, protected fields | Firebase Emulator, Vitest | każda zmiana Rules |
| Functions checks | build, lint, auth, role i payload validation | TypeScript, ESLint | każda zmiana `functions/src` |
| Admin panel checks | build, lint, format, podstawowa dostępność | Vite, TypeScript, ESLint | każda zmiana panelu |
| Manual smoke | podstawowe flow na urządzeniu | manual release test plan | przed releasem i po zmianach przekrojowych |
| Performance/load | wydajność, koszty, limity, UX | checklisty QA, Firebase Performance | przed RC i po zmianach wydajnościowych |

## Minimalna bramka PR

Android:

```powershell
$env:MAPS_API_KEY="AIzaSyPlaceholder"; .\gradlew.bat testDebugUnitTest
$env:MAPS_API_KEY="AIzaSyPlaceholder"; .\gradlew.bat detekt
$env:MAPS_API_KEY="AIzaSyPlaceholder"; .\gradlew.bat lint
```

Firestore i Storage Rules:

```powershell
cd tests/firestore-rules
npm test
```

Cloud Functions:

```powershell
cd functions
npm run lint
npm run build
```

Panel administracyjny:

```powershell
cd admin-panel
npm run format:check
npm run lint
npm run build
npm run a11y:check
```

## Krytyczne ścieżki

| Ścieżka | Automatycznie | Manualnie przed releasem |
| --- | --- | --- |
| Rejestracja i logowanie | walidacja, sukces, błędy auth, profil public/private | utworzenie konta, restart, logout |
| Profil | load/update, pola prywatne, błędy | edycja, avatar, prywatność |
| Dodawanie miejsca | walidacja, limity zdjęć, duplikaty, zapis | miejsce bez zdjęć i ze zdjęciem |
| Opinie i oceny | walidacja, ownership, limity | dodanie, edycja, usunięcie, zdjęcia |
| Lista i wyszukiwanie | sortowanie, paging, debounce, puste stany | filtry, sortowanie, offline |
| Mapa | bounds, debounce, cache, clustering, fallback | zoom, markery, brak lokalizacji |
| Ranking | limity, filtry, Remote Config fallback | ranking i pusty stan |
| Runtime permissions | logika stanów, trwała odmowa, retry | wszystkie punkty wejścia i settings return |
| Offline/cache | read cache, retry, brak fałszywego sukcesu | restart offline, widget, logout |
| Account deletion | logika błędów, reauth, cleanup helpers | pełny test Auth, Firestore, Storage, FCM |
| Deep linki/FCM | walidacja argumentów i auth guards | wejście z linku i powiadomienia |
| Rules | pozytywne i negatywne scenariusze | deploy do projektu testowego |

## Scenariusze negatywne

Krytyczne flow powinny uwzględniać:

- brak internetu,
- timeout,
- wygaśniętą sesję,
- zwykłą i trwałą odmowę zgody,
- wyłączony GPS,
- double submit,
- race condition,
- nieprawidłowy deep link,
- częściowy cleanup danych,
- błąd Rules lub App Check,
- nieprawidłowy token FCM.

## Aktualne pokrycie

Repozytorium ma testy dla części logiki Androida oraz Firestore i Storage Rules. Cloud Functions i panel administracyjny mają głównie bramki build/lint/format; ich pokrycie testami jednostkowymi i flow powinno być rozwijane osobnymi PR-ami.

## Regression suite przed RC

Uruchom wszystkie bramki automatyczne, a następnie:

- [Manual release test plan](manual-release-test-plan.md),
- [Android permissions device matrix](android-permissions-device-matrix.md),
- [Google Play security checklist](google-play-security-checklist.md),
- checklistę performance/load dla zmian mapy, list, uploadu, rankingu lub Firebase,
- checklistę dostępności dla zmian UI.

## Smoke suite

- [ ] aplikacja startuje,
- [ ] logowanie lub rejestracja działa,
- [ ] Start pokazuje dane lub kontrolowany pusty stan,
- [ ] Lista i Mapa działają,
- [ ] szczegóły miejsca otwierają się,
- [ ] można dodać miejsce i opinię,
- [ ] Profil i logout działają,
- [ ] brak internetu jest obsłużony,
- [ ] odmowa uprawnień nie tworzy martwej akcji,
- [ ] Crashlytics nie pokazuje nowego krytycznego błędu.

## Zasady dodawania testów

- każda poprawka regresji dostaje test, który wcześniej by nie przeszedł,
- logika sortowania, limitów, retry, uprawnień i cleanup nie jest testowana tylko manualnie,
- testy Firebase są deterministyczne i nie używają produkcji,
- testy nie wymagają prawdziwych sekretów,
- instrumented i screenshot tests dodajemy dla stabilnych krytycznych flow,
- flaky test nie może być ignorowany bez issue i właściciela.

## Priorytety dalszego rozwoju

1. Unit tests Cloud Functions dla auth, ról i walidacji payloadów.
2. Testy panelu admina dla guardów i statusów.
3. Instrumented smoke dla auth i głównych zakładek.
4. Dodatkowe testy opinii i zdjęć.
5. Testy fallbacków Remote Config.
6. Testy account deletion i runtime permissions dla scenariuszy częściowego błędu.
