# Testing strategy and regression coverage

Powiazane issue: #176

## Cel

Ten dokument definiuje praktyczna strategię testów dla kidZone: aplikacji Android,
Firebase Rules, Cloud Functions i panelu administracyjnego. Ma pomagac decydowac,
co musi byc testowane automatycznie, co wystarczy sprawdzic manualnie i jaka
bramka powinna przejsc przed PR-em albo release candidate.

## Piramida testow

| Poziom | Cel | Narzedzia | Kiedy wymagany |
| --- | --- | --- | --- |
| Unit tests | Logika ViewModeli, mappery, walidacje, sortowanie, limity, retry, offline sync | JUnit 5, MockK, kotlinx-coroutines-test | Kazda zmiana logiki aplikacji |
| Rules tests | Uprawnienia Firestore i Storage, anty-spoofing, pola chronione | Vitest, Firebase Rules Unit Testing, Firebase Emulator | Kazda zmiana `firestore.rules` albo `storage.rules` |
| Functions checks | TypeScript build, ESLint, auth/context checks dla callable/background functions | `npm run build`, `npm run lint`, docelowo unit tests | Kazda zmiana w `functions/src` |
| Admin panel checks | TypeScript build, lint, format, podstawowe a11y label checks | Vite, TypeScript, ESLint, Prettier | Kazda zmiana w `admin-panel/src` |
| Manual smoke | Czy aplikacja dziala jako calosc na urzadzeniu/emulatorze | `docs/qa/manual-release-test-plan.md` | Przed releasem i po zmianach cross-cutting |
| Manual performance/load | UX, Firebase Performance, Crashlytics, limity i koszty | `docs/qa/performance-test-checklist.md` | Przed release candidate i po zmianach wydajnosciowych |

## Minimalna bramka PR

Dla zwyklego PR w Androidzie:

```powershell
$env:MAPS_API_KEY="AIzaSyPlaceholder"; .\gradlew.bat testDebugUnitTest
$env:MAPS_API_KEY="AIzaSyPlaceholder"; .\gradlew.bat detekt
```

Dla zmian Firestore/Storage Rules:

```powershell
cd tests/firestore-rules
npm test
```

Dla zmian Cloud Functions:

```powershell
cd functions
npm run lint
npm run build
```

Dla zmian panelu admina:

```powershell
cd admin-panel
npm run format:check
npm run lint
npm run build
npm run a11y:check
```

Dla zmian release/security/performance wykonaj tez odpowiednia checklistę:

- [Manual release test plan](manual-release-test-plan.md),
- [Performance and load test checklist](performance-test-checklist.md),
- [Accessibility TalkBack checklist](accessibility-talkback-checklist.md), jesli zmiana dotyka UI.

## Krytyczne sciezki uzytkownika

| Sciezka | Minimalne pokrycie automatyczne | Manualny smoke przed release |
| --- | --- | --- |
| Rejestracja | walidacja formularza, sukces repo, blad auth, utworzenie profilu public/private | zalozenie konta testowego i sprawdzenie dokumentow Firestore |
| Logowanie | sukces, bledne haslo, brak usera, komunikaty bledow | login, restart aplikacji, logout |
| Profil | load/update profilu, prywatne pola, wylogowanie | edycja profilu, avatar/fallback, prywatnosc danych |
| Dodawanie miejsca | walidacje, limity zdjec/nazwy, duplikaty w poblizu, zapis repo, offline retry | dodanie miejsca bez zdjec i ze zdjeciem |
| Opinie i oceny | walidacje, protected fields, owner rules, limity tekstu/zdjec | dodanie/edycja/usuniecie opinii, zdjecia opinii |
| Lista/search/filtry | sortowanie, paging, search debounce/filter, puste stany | wyszukiwanie, kategorie, brak internetu |
| Mapa | viewport debounce/cache, limit markerow, fallback listy, a11y labels | przesuwanie/zoom, marker -> szczegoly, fallback |
| Ranking | filtrowanie aktywnych wpisow, limity Remote Config, badge context | ranking miejsc/uzytkownikow, pusty stan |
| Firestore Rules | odczyty/zapisy users, places, reviews, reports, admin claims | deploy rules do projektu testowego i smoke aplikacji |
| Storage Rules | upload zdjec miejsca/opinii, typ MIME, limit rozmiaru, sciezka ownera | upload zdjec w aplikacji |
| Cloud Functions | auth context, walidacja payloadu, idempotencja, brak spoofingu | testowe wywolanie flow zalezne od funkcji |
| Admin panel | build/lint/format, role-based UI guards, status labels | login admina, reports/users/places workflow |

## Aktualne pokrycie w repo

Android ma juz podstawowe testy jednostkowe dla:

- logowania i rejestracji,
- dodawania miejsca,
- repository opinii,
- listy miejsc,
- mapy, klastrów i labeli dostępności,
- profilu,
- cold start/performance helperow,
- offline sync,
- mapperow bledow, geohash, hasla i normalizacji tekstu.

Firebase Rules maja testy dla Firestore i Storage w `tests/firestore-rules`.

Cloud Functions i panel admina maja obecnie bramki build/lint/format, ale nie
maja jeszcze testow jednostkowych/specow user-flow. To jest swiadoma luka do
domkniecia osobnymi PR-ami.

## Regression suite

Przed release candidate uruchom:

```powershell
$env:MAPS_API_KEY="AIzaSyPlaceholder"; .\gradlew.bat testDebugUnitTest
$env:MAPS_API_KEY="AIzaSyPlaceholder"; .\gradlew.bat detekt
```

```powershell
cd tests/firestore-rules
npm test
```

```powershell
cd functions
npm run lint
npm run build
```

```powershell
cd admin-panel
npm run format:check
npm run lint
npm run build
npm run a11y:check
```

Nastepnie wykonaj manualnie:

- sekcje 1-10 z [Manual release test plan](manual-release-test-plan.md),
- App Check/Firebase/security smoke z sekcji 12-13,
- performance/load checklist, jesli zmiana dotyka mapy, list, uploadu, rankingow albo Firebase.

## Smoke suite

Minimalny smoke przed mniejszym releasem albo po pilnym hotfixie:

- [ ] aplikacja startuje i pokazuje ekran startowy,
- [ ] rejestracja albo logowanie testowego konta dziala,
- [ ] Start pokazuje miejsca lub czytelny pusty stan,
- [ ] lista miejsc otwiera szczegoly,
- [ ] mapa pokazuje fallback albo markery i nie crashuje,
- [ ] ranking laduje dane albo pusty stan,
- [ ] profil laduje dane i pozwala sie wylogowac,
- [ ] mozna dodac miejsce bez zdjec,
- [ ] mozna dodac opinie,
- [ ] brak internetu pokazuje kontrolowany blad,
- [ ] Crashlytics nie pokazuje nowego krytycznego crasha po smoke.

## Zasady dodawania testow

- Kazda poprawka regresji powinna dostac test, ktory najpierw by nie przeszedl.
- Logika sortowania, limitow, retry i uprawnien nie powinna byc testowana tylko manualnie.
- UI screenshot/instrumented tests dodajemy dopiero dla stabilnych, krytycznych przeplywow.
- Testy emulatorow Firebase musza byc deterministyczne i nie moga uzywac produkcyjnego projektu.
- Testy nie powinny wymagac prawdziwych sekretow; `MAPS_API_KEY` moze byc placeholderem dla unit testow.

## Priorytety kolejnych PR-ow

1. Cloud Functions unit tests dla autoryzacji i walidacji payloadow.
2. Admin panel testy podstawowych guardow i statusow.
3. Instrumented smoke test dla logowania i glownych zakladek.
4. Dodatkowe testy dla opinii/zdjec, szczegolnie limity i edycja.
5. Regression tests dla Remote Config fallbackow i krytycznych parametrow.
