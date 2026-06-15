# kidZone 🐻

Społecznościowa aplikacja mobilna dla rodziców — odkrywaj, dodawaj i oceniaj miejsca przyjazne dzieciom.

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

## 🔒 Bezpieczeństwo

- Firebase App Check (Play Integrity + reCAPTCHA Enterprise)
- Auth verification na wszystkich admin Cloud Functions
- Input sanitization (escapeHtml) w emailach
- Firestore Security Rules z walidacją typów i ownershipem
- Storage Rules z limitami rozmiaru i MIME
- CSP headers na hostingu
- ProGuard/R8 w release (zawężone -keep reguły + dontwarn dla wewnętrznych klas play-services)
- allowBackup=false
- Network Security Config (no cleartext)
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

## 📄 Licencja

Projekt prywatny.
