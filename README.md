# kidZone 🐻

Społecznościowa aplikacja mobilna dla rodziców — odkrywaj, dodawaj i oceniaj miejsca przyjazne dzieciom.

## 📋 Wymagania systemowe

| Komponent | Wersja |
|-----------|--------|
| Android min SDK | **26** (Android 8.0 Oreo) |
| Android target SDK | **35** (Android 15) |
| Java / JDK | **17** |
| Kotlin | **2.0.20** |
| Gradle | **8.13.2** (AGP) |
| Node.js (Functions) | **22** |
| Firebase CLI | najnowsza (`npm i -g firebase-tools`) |

## 📱 Funkcje

### Dla użytkowników:
- 🗺️ Mapa miejsc przyjaznych dzieciom w okolicy (Google Maps)
- 📍 Dodawanie nowych miejsc z kategoriami i udogodnieniami
- ⭐ Opinie i oceny (1-5 gwiazdek + komentarz + zdjęcia)
- 🏆 System odznak i rankingów (użytkownicy + miejsca)
- 📷 Galeria zdjęć miejsc
- 🔔 Powiadomienia push (nowa opinia, nowe zdjęcie, ranking)
- 👤 Profil użytkownika z edycją danych
- 🔍 Wyszukiwanie miejsc
- 📊 Ranking TOP 10 użytkowników i miejsc
- 🚨 Zgłaszanie naruszeń (miejsca, opinie, zdjęcia)
- 💡 Propozycje zmian w danych miejsc
- 📲 Widget "Miejsca w pobliżu" na ekranie głównym (Glance AppWidget)
- 🔄 In-App Update — automatyczne powiadomienie o nowej wersji
- ⭐ In-App Review — zachęta do oceny w Google Play
- 📴 Tryb offline — cache Room + synchronizacja w tle (WorkManager)
- 🔗 Deep linking (`https://playground-705e7162.web.app/place/{id}` + `kidzone://place/{id}`)
- 🧪 A/B Testing (Remote Config + Analytics)
- 🛑 Maintenance mode (Remote Config gate)

### Panel administracyjny (React):
- 📊 Dashboard ze statystykami
- 🚨 Zarządzanie zgłoszeniami (miejsca, opinie, zdjęcia) — paginacja server-side
- 📝 Zatwierdzanie/odrzucanie propozycji zmian
- 🏠 Zarządzanie miejscami (edycja, usuwanie z powodem)
- 👥 Zarządzanie użytkownikami — paginacja, filtrowanie, blokowanie
- 🔒 Pełne zabezpieczenie — auth check na Cloud Functions

## 🛠️ Tech Stack

### Android:
- **Kotlin** + Jetpack Compose
- **Hilt** (Dependency Injection)
- **Firebase** Auth + Firestore + Storage + Crashlytics + FCM + App Check + Remote Config + Performance
- **Google Maps** SDK
- **Room** (offline cache)
- **Material Design 3**
- **JUnit 5** + MockK + Turbine (testing)
- **LeakCanary** (debug memory leak detection)

### Panel admina:
- **React 19** + TypeScript
- **Vite 6**
- **Material UI 6**
- **Firebase SDK 11**
- **React Router 7**
- **ESLint 9** (flat config + typescript-eslint + react-hooks)
- **Prettier 3** (singleQuote, trailingComma, printWidth:100)

### Backend:
- **Firebase Cloud Functions** (Node.js 22 / TypeScript)
- **Nodemailer** (email notifications)
- **Firebase Hosting** (SPA + static pages)

### CI/CD (GitHub Actions):
- **Android CI** — lint + assembleDebug + unit tests (JDK 17, Gradle caching)
- **Cloud Functions CI** — ESLint + tsc + build
- **Admin Panel CI** — ESLint + Prettier + tsc + vite build
- **Firestore Rules Tests** — vitest + @firebase/rules-unit-testing + emulator

## 🏷️ Kategorie i udogodnienia

### Kategorie miejsc (7):
| Enum | Opis |
|------|------|
| `PLAYGROUND` | Plac zabaw (outdoor) |
| `PLAY_ROOM` | Sala zabaw (indoor) |
| `CAFE` | Kawiarnia |
| `RESTAURANT` | Restauracja |
| `PARK` | Park |
| `ATTRACTION` | Atrakcja (zoo, muzeum, aquapark itp.) |
| `OTHER` | Inne |

### Udogodnienia (40+):
Każde udogodnienie jest przypisane do odpowiednich kategorii — UI filtruje listę po wybranej kategorii.

| Grupa | Przykłady |
|-------|-----------|
| **Uniwersalne** | Przewijak, toaleta, dostęp dla wózka, parking |
| **Plac zabaw** | Ogrodzenie, miękka nawierzchnia, zadaszone ławki, strefa malucha, strefa bez aut |
| **Restauracja / Kawiarnia** | Menu dla dzieci, krzesełko, sztućce dla dzieci, szybka obsługa, kącik zabaw widoczny z sali |
| **Sala zabaw** | Strefy wiekowe, animator, monitoring, dezynfekcja zabawek, strefa rodzica, szafki |
| **Park** | Strefa piknikowa, bezpieczne ścieżki, woda pitna, miejsce do karmienia piersią, oświetlenie |
| **Atrakcja** | Wypożyczalnia wózków, strefy odpoczynku, fast-track dla rodzin, pokój matki z dzieckiem, punkt zgubionego dziecka |

Pełna lista: `domain/model/Amenity.kt` • Kategorie: `domain/model/PlaceCategory.kt`

## 📐 Architektura

```
┌─────────────────────────────────────────────────────┐
│                   Presentation                       │
│  ViewModels → Compose Screens → Navigation          │
│  (czyste od android.content.Context)                │
├─────────────────────────────────────────────────────┤
│                   Domain                             │
│  Models │ Repository interfaces │ Use Cases          │
│  Service interfaces (LocationProvider,              │
│  ImageCompressorPort, BadgePreferences)             │
├─────────────────────────────────────────────────────┤
│                   Data                               │
│  Firestore repos │ Room cache │ Remote Config       │
│  Android service implementations                    │
├─────────────────────────────────────────────────────┤
│                   Framework                          │
│  Hilt DI │ Firebase SDK │ Google Play Services      │
└─────────────────────────────────────────────────────┘
```

ViewModele nie importują `android.content.Context` — zależą od abstrakcji
w warstwie domain (`LocationProvider`, `ImageCompressorPort`, `BadgePreferences`).
Implementacje Android są w `data/service/` i bindowane przez Hilt (`di/ServiceModule`).

## 📝 Dokumentacja kodu (KDoc)

Projekt używa **KDoc** — kotlinowy odpowiednik Javadoc:

```kotlin
/**
 * Kompresuje zdjęcie z podanego URI do WebP ByteArray.
 *
 * @param uri URI zdjęcia (z photo pickera lub kamery)
 * @return ByteArray skompresowanego WebP, lub null jeśli decode się nie powiódł
 * @throws IllegalArgumentException gdy URI jest nieprawidłowy
 * @see ImageCompressor pełny pipeline kompresji
 */
fun compressToWebp(uri: Uri): ByteArray?
```

### Tagi KDoc:
| Tag | Opis | Przykład |
|-----|------|---------|
| `@param` | Parametr funkcji | `@param placeId identyfikator miejsca` |
| `@return` | Wartość zwracana | `@return lista miejsc lub pusty list` |
| `@throws` | Wyjątek | `@throws TimeoutException po 30s` |
| `@property` | Pole data class | `@property name nazwa użytkownika` |
| `@see` | Odnośnik do innej klasy/metody | `@see PlaceRepository` |
| `@sample` | Przykład użycia | `@sample com.kidzone.samples.addPlace` |
| `@since` | Od której wersji | `@since 0.2.0` |

### Generowanie HTML:
```bash
# Generuj dokumentację HTML (wymaga Dokka plugin)
./gradlew dokkaHtml
# Output: app/build/dokka/html/index.html
```

## 🧪 A/B Testing

Framework eksperymentowy oparty na Firebase Remote Config + Analytics:

```kotlin
// W ViewModelu:
val variant = experimentManager.getVariant(ActiveExperiments.HOME_LAYOUT)
experimentManager.logExposure(ActiveExperiments.HOME_LAYOUT)

// W Compose:
ExperimentSwitch(
    experimentManager = experimentManager,
    experiment = ActiveExperiments.HOME_LAYOUT,
    control = { HomeLayoutCurrent() },
    treatment = { HomeLayoutNew() }
)
```

Aktywne eksperymenty definiowane w `experiment/ActiveExperiments.kt`.
Szczegóły: patrz `experiment/` package.

## 🔢 Wersjonowanie

Aplikacja używa automatycznego systemu wersjonowania opartego na Git:

### `versionCode` (numer buildu)
Generowany automatycznie jako **liczba commitów na HEAD**:
```kotlin
versionCode = providers.exec {
    commandLine("git", "rev-list", "--count", "HEAD")
}.standardOutput.asText.get().trim().toIntOrNull() ?: 1
```
Każdy commit na `main` automatycznie podnosi `versionCode` — nie trzeba go ręcznie bumpować.

### `versionName` (wersja widoczna dla użytkownika)
Format: `MAJOR.MINOR.PATCH` na `main`, z suffixem `-dev#<nr>` na feature branchach:

| Branch | Przykład `versionName` |
|--------|----------------------|
| `main` | `1.0.0` |
| `feature/72-nowy-ekran` | `1.0.0-dev#72` |
| CI z `PR_NUMBER=85` | `1.0.0-dev#85` |
| Inny (fallback) | `1.0.0-dev#a3f4b2c` (skrócony SHA) |

Aby zmienić wersję bazową, edytuj `baseVersion` w `app/build.gradle.kts`.

## 🚀 Setup

### Android:
1. Skopiuj `google-services.json.template` do `google-services.json` i uzupełnij
2. W `local.properties` dodaj: `MAPS_API_KEY=AIza...`
3. Build: `./gradlew assembleDebug`
4. Testy: `./gradlew testDebugUnitTest`

### Panel admina:
```bash
cd admin-panel
npm install
cp .env.example .env  # uzupełnij klucze Firebase
npm run dev           # development server
npm run lint          # ESLint
npm run format        # Prettier auto-fix
npm run format:check  # Prettier dry-run
```

### Cloud Functions:
```bash
cd functions
npm install
npm run build
```

### Firestore Rules Tests:
```bash
cd tests/firestore-rules
npm install
firebase emulators:exec --only firestore --project kidzone-rules-test "npx vitest --run"
```

### Deploy Firebase:
```bash
firebase deploy --only functions,firestore:rules,firestore:indexes,storage,hosting:app
```

### Staging:
```bash
firebase use staging    # przełącz na playground-705e7162-staging
firebase deploy         # deploy na staging
firebase use default    # wróć na produkcję
```

Szczegóły: [STAGING.md](./STAGING.md)

## 📱 Uprawnienia (Permissions)

| Uprawnienie | Cel | Wymagane? |
|-------------|-----|-----------|
| `INTERNET` | Komunikacja z Firebase (Auth, Firestore, Storage, FCM) | Tak |
| `ACCESS_NETWORK_STATE` | Sprawdzenie dostępności sieci (offline mode) | Tak |
| `ACCESS_FINE_LOCATION` | Lokalizacja użytkownika na mapie, "miejsca w pobliżu" | Tak |
| `ACCESS_COARSE_LOCATION` | Przybliżona lokalizacja (fallback) | Tak |
| `CAMERA` | Robienie zdjęć miejsc bezpośrednio z appki | Nie (`required=false`) |
| `READ_MEDIA_IMAGES` | Wybór zdjęć z galerii (Android 13+) | Tak |
| `POST_NOTIFICATIONS` | Powiadomienia push (FCM) — Android 13+ wymaga runtime permission | Tak |

### Hardware features (opcjonalne):
- `android.hardware.camera` — `required=false` (appka działa bez kamery)
- `android.hardware.camera.autofocus` — `required=false`
- `android.hardware.location` — `required=false` (użytkownik może przeglądać bez GPS)

## 🔒 Bezpieczeństwo

- Firebase App Check (Play Integrity + reCAPTCHA Enterprise)
- Auth verification na wszystkich admin Cloud Functions
- Input sanitization (escapeHtml) w emailach
- Firestore Security Rules z walidacją typów i ownershipem
- Storage Rules z limitami rozmiaru i MIME
- Content Security Policy (CSP) headers na hostingu
- ProGuard/R8 w release (zawężone -keep reguły + dontwarn dla wewnętrznych klas play-services)
- allowBackup=false
- Network Security Config (no cleartext)
- Release signing config (keystore z local.properties / env vars)
- 1 zgłoszenie per user per target (duplicate prevention)

## 📁 Struktura projektu

```
├── app/                          # Android app (Kotlin/Compose)
│   ├── src/main/java/com/kidzone/
│   │   ├── analytics/            # Firebase Analytics helper
│   │   ├── data/
│   │   │   ├── local/            # Room database, DAOs, entities
│   │   │   ├── remote/           # RemoteConfigService, DTOs
│   │   │   ├── repository/       # Firestore implementations
│   │   │   └── service/          # Android implementations (Location, Image, Prefs)
│   │   ├── di/                   # Hilt modules (ServiceModule, DatabaseModule)
│   │   ├── domain/
│   │   │   ├── model/            # Domain models (Place, Review, User)
│   │   │   ├── repository/       # Repository interfaces
│   │   │   ├── service/          # Domain service interfaces
│   │   │   └── usecase/          # Use cases (ComputeBadges, NotificationPrefs)
│   │   ├── experiment/           # A/B Testing framework
│   │   ├── messaging/            # FCM push service
│   │   ├── navigation/           # NavGraph, Routes
│   │   ├── presentation/         # Compose screens + ViewModels
│   │   │   ├── auth/             # Login, Register
│   │   │   ├── home/             # Home screen
│   │   │   ├── maintenance/      # Maintenance mode screen
│   │   │   ├── map/              # Map screen
│   │   │   ├── onboarding/       # Onboarding
│   │   │   ├── place/            # Add/Edit/Details/List/MyPlaces
│   │   │   ├── profile/          # Profile, badges, notifications
│   │   │   ├── ranking/          # Rankings
│   │   │   ├── review/           # MyReviews
│   │   │   └── splash/           # Splash screen
│   │   ├── logging/              # CrashlyticsTree
│   │   └── utils/                # AppConfig, ImageCompressor, TextNormalization
│   ├── src/test/                 # Unit tests (JUnit 5 + MockK + Turbine)
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── admin-panel/                  # React admin panel
│   ├── src/
│   │   ├── pages/                # Dashboard, Reports, Places, Users, ChangeRequests
│   │   │   └── reports/          # Decomposed table components
│   │   ├── components/           # Layout
│   │   ├── hooks/                # useAuth
│   │   ├── services/             # firebase, api, cloudFunctions
│   │   └── types/                # TypeScript interfaces
│   ├── eslint.config.js          # ESLint flat config
│   ├── .prettierrc               # Prettier config
│   └── package.json
├── functions/                    # Cloud Functions (Node 22/TypeScript)
│   └── src/index.ts
├── tests/
│   └── firestore-rules/          # Firestore rules unit tests (vitest)
├── .github/workflows/            # CI/CD
│   ├── android.yml
│   ├── functions.yml
│   ├── admin-panel.yml
│   └── firestore-rules.yml
├── gradle/libs.versions.toml     # Version catalog (all deps in one place)
├── firestore.rules
├── firestore.indexes.json
├── storage.rules
├── firebase.json
├── .firebaserc                   # Project aliases (default + staging)
└── STAGING.md                    # Staging environment docs
```

## 📧 Cloud Functions

| Trigger | Opis |
|---------|------|
| onPlaceReport | Email do admina o nowym zgłoszeniu miejsca |
| onPlaceChangeRequest | Email o propozycji zmiany |
| onUserCreated | Email powitalny + powiadomienie admina |
| onReviewReport | Email o zgłoszeniu opinii |
| onPhotoReport | Email o zgłoszeniu zdjęcia + przyciski akcji |
| onUserDeleted | Email pożegnalny + powiadomienie admina |
| onReviewCreatedPush | Push do właściciela miejsca o nowej opinii |
| onBadgeEarned | Push o nowej odznace |
| onPhotoAddedToPlace | Push o nowym zdjęciu |
| onUserBanned | Push + email o blokadzie konta |
| dailyRankingCheck | Push o awansie w rankingu (scheduled) |
| adminDeletePlace | HTTP: usuń miejsce + email z powodem |
| adminDeleteReview | HTTP: usuń opinię + email z powodem |
| adminDeletePhoto | HTTP: usuń zgłoszone zdjęcie + email |
| adminDeletePhotoFromPlace | HTTP: usuń zdjęcie z miejsca + email |
| adminDeleteUser | HTTP: usuń użytkownika + email z powodem |
| adminDismissPhotoReport | HTTP: odrzuć zgłoszenie zdjęcia |
| adminUpdateUserEmail | HTTP: aktualizuj email (Auth + Firestore) |

## 🏗️ Conventions

### Commit messages:
```
feat: nowa funkcjonalność
fix: naprawa buga
refactor: zmiana struktury bez zmiany zachowania
perf: optymalizacja wydajności
style: formatowanie (Prettier, whitespace)
docs: dokumentacja
test: testy
chore: tooling, CI, deps
cleanup: usuwanie dead code
devops: CI/CD, deploy config
```

### Branching:
- `main` — stabilny, produkcyjny kod
- `feature/*` — nowe funkcje
- `fix/*` — poprawki bugów
- `refactor/*` — refactoring
- `cleanup/*` — cleanup & polish
- `devops/*` — CI/CD zmiany
- `style/*` — formatowanie

## 📋 Regulamin i Polityka Prywatności

- Regulamin: `/terms-of-service`
- Polityka prywatności: `/privacy-policy`

## 💰 Koszty

| Usługa | Plan | Limit free |
|--------|------|-----------|
| Firebase Auth | Spark | 50k MAU |
| Firestore | Spark | 50k reads/20k writes/day |
| Cloud Functions | Spark | 2M invocations/month |
| Firebase Storage | Spark | 5GB |
| Firebase Hosting | Spark | 10GB storage, 360MB/day |
| Google Maps SDK | — | $200/month credit (~28k loads) |
| Play Integrity | — | 10k requests/day |

## 🚀 Release / Google Play Store

### Budowanie release bundle:
```bash
./gradlew bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab
```

### Signing config:
Release build wymaga keystore. Skonfiguruj w `local.properties` (lub CI env vars):
```properties
KIDZONE_KEYSTORE_FILE=/path/to/kidzone-release.keystore
KIDZONE_KEYSTORE_PASSWORD=***
KIDZONE_KEY_ALIAS=kidzone
KIDZONE_KEY_PASSWORD=***
```

### Checklist przed uploadem do Play Console:
1. ✅ `./gradlew bundleRelease` przechodzi bez błędów
2. ✅ `versionCode` jest wyższy niż poprzedni upload
3. ✅ `google-services.json` — produkcyjny (nie staging!)
4. ✅ ProGuard mapping: `app/build/outputs/mapping/release/mapping.txt` → upload do Play Console (Crashlytics)
5. ✅ Testuj na fizycznym urządzeniu z release buildem
6. ✅ Sprawdź deep linki (`adb shell am start -d "kidzone://place/testId"`)

### Materiały do listingu:
| Element | Wymiary | Format |
|---------|---------|--------|
| Ikona | 512×512 px | PNG, 32-bit |
| Feature graphic | 1024×500 px | PNG / JPEG |
| Screenshoty (phone) | 16:9 (np. 1080×1920) | PNG / JPEG, min 2, max 8 |
| Screenshoty (tablet) | 16:9 (np. 1920×1200) | PNG / JPEG (opcjonalne) |

### Google Play dane:
| Pole | Wartość |
|------|---------|
| Kategoria | Parenting |
| Content rating | PEGI 3 / Everyone (IARC questionnaire) |
| Target audience | Rodzice (18+) — **nie** zaznaczaj "dzieci" |
| Polityka prywatności | `https://playground-705e7162.web.app/privacy-policy` |
| Reklamy | Nie |
| In-app purchases | Nie |

## ⚠️ Troubleshooting

### `bundleRelease` / R8 missing classes
Jeśli `./gradlew bundleRelease` (lub `assembleRelease`) kończy się błędem:
```
ERROR: Missing classes detected while running R8.
```
Upewnij się, że `proguard-rules.pro` zawiera:
```proguard
-dontwarn com.google.android.gms.internal.**
-dontwarn com.google.android.gms.common.annotation.NoNullnessRewrite
```
Te klasy to wewnętrzne adnotacje Google Play Services, które nie są potrzebne w runtime.

Alternatywnie, sprawdź plik wygenerowany przez R8:
```
app/build/outputs/mapping/release/missing_rules.txt
```
i dodaj wymienione tam reguły do `proguard-rules.pro`.

### Release signing nie działa
Sprawdź, że w `local.properties` (lub env vars) ustawione są:
```
KIDZONE_KEYSTORE_FILE, KIDZONE_KEYSTORE_PASSWORD, KIDZONE_KEY_ALIAS, KIDZONE_KEY_PASSWORD
```
Build Gradle wypisze warning jeśli brakuje któregoś z tych kluczy.

## 📄 Licencja

Projekt prywatny.
