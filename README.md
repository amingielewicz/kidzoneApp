# kidZone 🐻

Społecznościowa aplikacja mobilna dla rodziców i opiekunów. Ułatwia odkrywanie, dodawanie i ocenianie miejsc przyjaznych dzieciom.

## Najważniejsze funkcje

### Aplikacja Android

- mapa miejsc z obsługą klastrów i lokalizacji użytkownika,
- lista miejsc z wyszukiwaniem, sortowaniem i filtrami,
- kategorie i rozbudowany zestaw udogodnień,
- dodawanie oraz edycja miejsc,
- opinie, oceny i zdjęcia,
- profile użytkowników, odznaki i rankingi,
- zgłoszenia naruszeń oraz propozycje zmian,
- powiadomienia push,
- widget „Miejsca w pobliżu”,
- tryb offline oparty na Room i WorkManager,
- deep linki do szczegółów miejsca,
- In-App Update i In-App Review,
- A/B Testing oraz maintenance mode przez Firebase Remote Config.

Karty miejsc korzystają ze wspólnego komponentu `CategoryBadge`, dzięki czemu oznaczenia kategorii są spójne na ekranie głównym i liście miejsc.

### Panel administracyjny

- dashboard i statystyki,
- zarządzanie miejscami i użytkownikami,
- obsługa zgłoszeń miejsc, opinii i zdjęć,
- zatwierdzanie oraz odrzucanie propozycji zmian,
- autoryzacja operacji administracyjnych przez Cloud Functions.

## Wymagania

| Komponent | Wersja |
| --- | --- |
| Android min SDK | 26 (Android 8.0) |
| Android target SDK | 35 (Android 15) |
| JDK | 17 |
| Kotlin | 2.0.20 |
| Android Gradle Plugin | 8.13.2 |
| Node.js dla Cloud Functions | 22 |

## Stack technologiczny

### Android

- Kotlin i Jetpack Compose,
- Material Design 3,
- Hilt,
- Firebase Auth, Firestore, Storage, FCM, Crashlytics, App Check, Remote Config i Performance,
- Google Maps SDK,
- Room i WorkManager,
- JUnit 5, MockK i Turbine,
- LeakCanary w buildach debug.

### Panel administracyjny

- React 19 i TypeScript,
- Vite 6,
- Material UI 6,
- Firebase SDK 11,
- React Router 7,
- ESLint 9 i Prettier 3.

### Backend i infrastruktura

- Firebase Cloud Functions w TypeScript,
- Firebase Hosting,
- Firestore i Cloud Storage,
- GitHub Actions.

## Architektura

Projekt stosuje podział na warstwy:

```text
Presentation
    Compose Screens, ViewModels, Navigation

Domain
    modele, interfejsy repozytoriów, use case'y i porty usług

Data
    repozytoria Firestore, cache Room, Remote Config i implementacje usług Android

Framework
    Hilt, Firebase SDK i Google Play Services
```

ViewModele nie zależą bezpośrednio od `android.content.Context`. Funkcje platformowe są wystawione przez interfejsy warstwy domain i implementowane w warstwie data.

Wszystkie kluczowe komponenty posiadają ustrukturyzowaną dokumentację KDoc (🎯 Odpowiedzialności, 📥 Wejście, 📤 Wyjście, ✅ Gwarancje), co ułatwia onboarding i utrzymanie spójności architektonicznej.

Szczegóły:

- [indeks dokumentacji](docs/README.md),
- [standardy dokumentacji KDoc (Android)](docs/api/VIEWMODEL_KDOC_STANDARD.md),
- [standardy dokumentacji (Admin Panel)](docs/api/ADMIN_PANEL_DOC_STANDARD.md),
- [standardy dokumentacji (Cloud Functions)](docs/api/CLOUD_FUNCTIONS_DOC_STANDARD.md),
- [architektura systemu](docs/architecture/ARCHITECTURE.md),
- [przepływ danych](docs/architecture/DATA_FLOW.md),
- [tryb offline](docs/android/OFFLINE_MODE.md),
- [nawigacja](docs/android/NAVIGATION.md).

## Uprawnienia Android

| Uprawnienie | Zastosowanie | Charakter |
| --- | --- | --- |
| `INTERNET` | Firebase, mapy i komunikacja sieciowa | wymagane |
| `ACCESS_NETWORK_STATE` | wykrywanie stanu sieci i obsługa offline | wymagane |
| `ACCESS_FINE_LOCATION` | dokładna lokalizacja podczas używania aplikacji | opcjonalne runtime |
| `ACCESS_COARSE_LOCATION` | lokalizacja przybliżona | opcjonalne runtime |
| `CAMERA` | wykonanie zdjęcia w aplikacji | opcjonalne runtime |
| `POST_NOTIFICATIONS` | powiadomienia na Androidzie 13+ | opcjonalne runtime |

Aplikacja nie deklaruje `ACCESS_BACKGROUND_LOCATION`, `READ_MEDIA_IMAGES` ani `READ_EXTERNAL_STORAGE`. Zdjęcia z galerii są wybierane przez systemowy Android Photo Picker.

Obsługa odmowy uprawnień jest współdzielona między ekranami. Po trwałej odmowie lokalizacji lub kamery aplikacja kieruje użytkownika do ustawień aplikacji zamiast ponownie wyświetlać nieskuteczny dialog systemowy.

Pełny opis i macierz QA:

- [Android permissions i zgodność z Google Play](docs/legal/android-permissions-play-compliance.md),
- [macierz testów uprawnień](docs/qa/android-permissions-device-matrix.md),
- [Data Safety](docs/legal/google-play-data-safety-draft.md).

## Uruchomienie projektu

### Android

1. Skopiuj `google-services.json.template` jako `google-services.json` i uzupełnij konfigurację Firebase.
2. Dodaj klucz map do `local.properties`:

```properties
MAPS_API_KEY=...
```

3. Zbuduj aplikację i uruchom testy:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

### Panel administracyjny

```bash
cd admin-panel
npm install
cp .env.example .env
npm run dev
```

Kontrole jakości:

```bash
npm run lint
npm run format:check
npm run build
```

### Cloud Functions

```bash
cd functions
npm install
npm run build
```

### Testy reguł Firestore

```bash
cd tests/firestore-rules
npm install
firebase emulators:exec --only firestore --project kidzone-rules-test "npx vitest --run"
```

## CI/CD

GitHub Actions obejmuje:

- lint, build i testy jednostkowe Androida,
- testy instrumentacyjne na emulatorze,
- skanowanie sekretów przez Gitleaks,
- budowanie release APK/AAB,
- kontrolę Cloud Functions,
- kontrolę panelu administracyjnego,
- testy reguł Firestore.

## Wersjonowanie i release

Wersja aplikacji znajduje się w `version.properties`:

```properties
VERSION_NAME=1.0.0
VERSION_CODE=1
```

Proces wydania:

1. Zwiększ `VERSION_NAME` i `VERSION_CODE`.
2. Uruchom testy oraz Android CI.
3. Zbuduj podpisany AAB.
4. Wykonaj ręczny smoke test na urządzeniu.
5. Wdróż wydanie stopniowo w Google Play.

Dokumentacja release:

- [wersjonowanie](docs/release/VERSIONING.md),
- [proces wydania](docs/release/RELEASE_PROCESS.md),
- [checklista Go/No-Go](docs/release/GO_NO_GO_CHECKLIST.md),
- [proces hotfix](docs/release/HOTFIX_PROCESS.md).

## Bezpieczeństwo i prywatność

- Firebase App Check,
- reguły Firestore i Storage z walidacją danych,
- kontrola autoryzacji w funkcjach administracyjnych,
- brak cleartext traffic,
- `allowBackup=false`,
- R8/ProGuard dla buildów release,
- Gitleaks w CI,
- ograniczone raportowanie błędów bez surowych danych wyjątków,
- walidacja argumentów deep linków.

Dokumenty:

- [security overview](docs/security.md),
- [Firebase security plan](docs/firebase-security-plan.md),
- [App Check](docs/app-check.md),
- [monitoring](docs/operations/MONITORING.md).

## Struktura repozytorium

```text
app/                    aplikacja Android
admin-panel/            panel administracyjny React
functions/              Firebase Cloud Functions
tests/firestore-rules/  testy reguł Firestore
docs/                   dokumentacja techniczna, produktowa, QA i release
.github/workflows/       pipeline'y CI/CD
```

## Kontrybucja

Nie commitujemy bezpośrednio do `main`. Każda zmiana przechodzi przez branch, pull request, zielony CI i squash merge.

Szczegóły: [CONTRIBUTING.md](CONTRIBUTING.md).

## Licencja

Projekt prywatny.
