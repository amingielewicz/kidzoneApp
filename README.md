# kidZone

> Mapa miejsc przyjaznych dzieciom — aplikacja mobilna Android.

kidZone to społecznościowa aplikacja dla rodziców i opiekunów. Pozwala
dodawać i oceniać miejsca przyjazne dzieciom (place zabaw, restauracje
z kącikiem dla dzieci, sale zabaw, parki, kawiarnie rodzinne), wyszukiwać
je na mapie, filtrować po kategorii i udogodnieniach, sprawdzać miejsca
najwyżej oceniane i te w okolicy.

Większość MVP jest już zaimplementowana — Firebase Auth (e-mail + Google),
Firestore z snapshot listenerami, Google Maps z customowymi pinezkami,
formularz dodawania/edycji/usuwania miejsc z GPS i reverse geocodingiem.
Pozostałe `TODO` (dodawanie opinii, ranking, statystyki profilu) są
wymienione w sekcji [Status MVP](#status-mvp).

## Stack

- **Kotlin 2.0.20** + **Jetpack Compose** (Material 3, BOM 2024.09.03)
- **Hilt 2.52** (DI) + KSP 2.0.20-1.0.25
- **Firebase** BOM 33.4.0 — Auth, Firestore, Storage
- **Google Maps** SDK 19.0.0 + `maps-compose` 4.4.1, `play-services-location` 21.3.0
- **Credential Manager** 1.3.0 + `googleid` 1.1.1 (nowoczesne Google Sign-In)
- **androidx.core:core-splashscreen** 1.0.1 (Splash Screen API Android 12+, backportowane)
- **Poppins** jako brand font (Google Font, OFL, bundlowany w `res/font/`, 3 weights: 400/600/700)
- **Coil 2.7.0** (obrazy), **Retrofit 2.11.0 + OkHttp** (na przyszłość), **Room 2.6.1** (cache offline)
- **Navigation Compose** 2.8.2, **Coroutines** 1.9.0
- Architektura: **MVVM + Clean Architecture** (warstwy `data` / `domain` / `presentation`)
- Build tooling: **AGP 8.13.2**, **Gradle 8.13** (przez `mise.toml`), Java 17

> Ikonki launcher / splash były generowane lokalnie przez Pillow + cairosvg
> z plików SVG (workflow developerski, opcjonalny). Do zbudowania aplikacji
> nie są wymagane.

## Branding i ikona

- **Splash screen** — dwustopniowy:
  1. Systemowy splash (Android 12+ Splash Screen API, backportowany przez
     `core-splashscreen`). Aktywowany przez `installSplashScreen()` w
     `MainActivity` przed `super.onCreate`. Theme `Theme.KidZone.Starting`
     z `windowSplashScreenBackground = #F5F8FB` i dedykowaną małą ikoną
     `ic_splash_screen_icon` (mieści się w okrągłej masce systemu, nie
     jest przycinana).
  2. Compose `SplashScreen` na tym samym tle `#F5F8FB` z logiem brandowym
     (`ic_splash_logo`, 75% szerokości) i `CircularProgressIndicator`.
     `SplashViewModel` decyduje na podstawie `AuthRepository.currentUser`
     czy idziemy do `Login`, czy `Main` (z minimum 800 ms display + 5 s
     timeout fallback na "niezalogowany").
- **Adaptive launcher icon** (`mipmap-anydpi-v26/ic_launcher.xml`) z trzema
  warstwami: `background`, `foreground` i `monochrome` — ten ostatni
  włącza **themed icons na Android 13+** (system tinten ikonkę pod
  wybraną tapetę / akcent użytkownika).
- **Brand font Poppins** — `displayLarge` / `headlineMedium` / `titleLarge`
  / `titleMedium` / `labelLarge` w Poppins, `bodyLarge` / `bodyMedium`
  zostają na systemowym SansSerif (czytelniejsze w długich tekstach
  jak adresy i opisy miejsc). Dzięki temu `TopAppBar` (używa `titleLarge`)
  od razu pokazuje napis "kidZone" w brand foncie.
- **Paleta marki** — żółty / niebieski / zielony / biały (zob. `ui/theme/Color.kt`).

## Struktura projektu

```
app/src/main/java/com/kidzone
├── KidZoneApplication.kt    # @HiltAndroidApp
├── MainActivity.kt          # @AndroidEntryPoint, installSplashScreen + Compose
│
├── data
│   ├── remote
│   │   ├── FirestoreCollections.kt    # users, places, reviews, photos
│   │   └── dto/                        # PlaceDto, ReviewDto, UserDto
│   └── repository
│       ├── FirebaseAuthRepository.kt   # e-mail + Google + reset, mapowanie błędów
│       ├── FirestorePlaceRepository.kt # observe (snapshot), top, near, add/update/delete (z timeoutem)
│       └── FirestoreReviewRepository.kt# observe (snapshot); add/report = TODO
│
├── domain
│   ├── model/               # User, Place, Review, Photo, PlaceCategory, Amenity
│   └── repository/          # interfejsy: AuthRepository, PlaceRepository, ReviewRepository
│
├── presentation
│   ├── splash/              # SplashScreen + SplashViewModel
│   ├── auth
│   │   ├── LoginScreen.kt       # e-mail + Google + reset
│   │   ├── LoginViewModel.kt
│   │   ├── RegisterScreen.kt    # nazwa + e-mail + hasło (min. 6)
│   │   ├── RegisterViewModel.kt
│   │   └── GoogleSignInLauncher.kt  # Credential Manager + GetGoogleIdOption
│   ├── main/MainScreen.kt   # shell z bottom navigation + FAB "+"
│   ├── home
│   │   ├── HomeScreen.kt        # hero, CTA mapa, Top, Blisko Ciebie
│   │   └── HomeViewModel.kt     # getTopPlaces + haversine sort dla nearby
│   ├── map
│   │   ├── MapScreen.kt         # GoogleMap + MarkerComposable + filtry + bottom sheet
│   │   └── MapViewModel.kt      # observePlaces + flatMapLatest po kategorii
│   ├── place
│   │   ├── list
│   │   │   ├── PlaceListScreen.kt        # LazyColumn + chipy quick + button "Filtry"
│   │   │   ├── PlaceListViewModel.kt     # observePlaces + filtr udogodnień AND
│   │   │   └── AmenityFilterSheet.kt     # grupowane sekcje, auto-expand po kategorii
│   │   ├── details
│   │   │   ├── PlaceDetailsScreen.kt     # karta główna + udogodnienia + opinie + edit/delete dla ownera
│   │   │   └── PlaceDetailsViewModel.kt  # getPlace + observeReviews + delete + autor
│   │   └── add
│   │       ├── AddPlaceScreen.kt         # formularz create/edit + GPS button
│   │       ├── AddPlaceViewModel.kt      # tryb create + edit (placeId), prune amenities po zmianie kategorii
│   │       └── LocationHelper.kt         # FusedLocation + Geocoder (Android 13+ async API)
│   ├── profile
│   │   ├── ProfileScreen.kt     # nazwa + email + Wyloguj (statystyki = TODO)
│   │   └── ProfileViewModel.kt
│   ├── ranking/RankingScreen.kt # placeholder (TODO)
│   └── common/CategoryStyle.kt  # ikona + kolor per PlaceCategory
│
├── ui/theme
│   ├── Color.kt             # paleta marki + light/dark surface
│   ├── Type.kt              # Poppins (Regular/SemiBold/Bold) + SansSerif body
│   └── Theme.kt             # KidZoneTheme — light + dark color scheme
│
├── navigation
│   ├── Routes.kt            # sealed class Route + AddPlace.create(placeId?)
│   └── NavGraph.kt          # KidZoneNavGraph + przejścia pre/post-auth
│
├── di
│   ├── FirebaseModule.kt    # @Provides FirebaseAuth/Firestore/Storage
│   └── RepositoryModule.kt  # @Binds dla 3 repo
│
└── utils
    ├── OpResult.kt          # sealed Success/Failure + map
    └── AuthException.kt     # typowane błędy auth (UserNotFound, InvalidCredentials, ...)
```

Resources:
- `res/values/{strings,colors,themes}.xml` — w tym `Theme.KidZone` i `Theme.KidZone.Starting`
- `res/drawable-nodpi/` — `ic_splash_logo.png`, `ic_splash_screen_icon.png`,
  `ic_launcher_foreground.png`, `ic_launcher_monochrome.png`
- `res/drawable/ic_launcher_background.xml`
- `res/mipmap-anydpi-v26/ic_launcher{,_round}.xml` — adaptive icon z monochrome
- `res/font/poppins_{regular,semibold,bold}.ttf` — bundlowany brand font

## Mapa schematu bazy

- Kolekcja `users` ↔ `domain.model.User` ↔ `data.remote.dto.UserDto`
- Kolekcja `places` ↔ `domain.model.Place` ↔ `data.remote.dto.PlaceDto`
- Kolekcja `reviews` ↔ `domain.model.Review` ↔ `data.remote.dto.ReviewDto`
  (top-level, z polem `placeId`; filtrowanie `whereEqualTo("placeId", ...)`
  bez wymagania composite indexa — `reportedAsSpam=false` filtrujemy
  po stronie klienta)
- Stała `FirestoreCollections.PHOTOS = "photos"` jest zarezerwowana, ale
  na dziś `photoUrls` jest trzymane jako lista URL-i bezpośrednio na
  dokumencie miejsca (z Firebase Storage).

## Uruchomienie lokalne

### 1. Wymagania

- Android Studio Koala (lub nowsze) z **JDK 17** (lub 21)
- **Gradle 8.13** — `mise.toml` w repo deklaruje wersję, więc
  `mise install` pobierze ją automatycznie. Wrapper Gradle też się zachowuje.
- **AGP 8.13.2**, `compileSdk = 35`, `minSdk = 26`, `targetSdk = 35`
- Konto Firebase + projekt z włączonymi: Authentication, Firestore, Storage
- Klucz Google Maps (Maps SDK for Android)

### 2. Konfiguracja Firebase

1. W konsoli Firebase utwórz projekt.
2. Dodaj aplikację Android z `applicationId = com.kidzone`.
3. Pobierz `google-services.json` i wrzuć do `app/google-services.json`.
   (Przykładowa struktura jest w `app/google-services.json.template`).
4. Włącz: Authentication (e-mail/hasło, Google), Cloud Firestore, Storage.
5. Dla logowania Google — dodaj SHA-1 fingerprint debug keystore
   (`./gradlew signingReport`), pobierz nowy `google-services.json`
   po włączeniu providera Google. Bez tego logowanie Google zwróci
   inline-message "Włącz Google Sign-In w Firebase Console...".

### 3. Konfiguracja klucza Google Maps

W pliku `local.properties` (gitignored z natury) dodaj:

```
MAPS_API_KEY=AIzaSy...twoj_klucz
```

Klucz jest podstawiany do `AndroidManifest.xml` jako `${MAPS_API_KEY}`.
Bez klucza mapa zarenderuje się jako szare/zielone tło i Logcat pokaże
`Authorization failure` z Maps SDK; build print też ostrzeże w czasie
konfiguracji.

Łańcuch rozwiązywania: `local.properties` → `gradle.properties`/`-PMAPS_API_KEY=...`
→ zmienna środowiskowa (przydatne w CI).

### 4. Build

```bash
./gradlew assembleDebug
```

### 5. Uprawnienia w runtime

`AndroidManifest.xml` deklaruje:
- `INTERNET`, `ACCESS_NETWORK_STATE` — Firebase / Maps
- `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` — natywny "Moja lokalizacja"
  na mapie, GPS w `AddPlaceScreen`, sekcja "Blisko Ciebie" na home
- `CAMERA`, `READ_MEDIA_IMAGES` — zarezerwowane pod uploady zdjęć (na razie
  nieużywane w kodzie)

Prośby o uprawnienia są wykonywane in-app: `MapScreen` automatycznie
przy pierwszym wejściu, `AddPlaceScreen` przy kliknięciu "Pobierz lokalizację",
`HomeScreen` przez kartę "Włącz lokalizację" w sekcji "Blisko Ciebie".

## Status MVP

| Funkcja                                       | Status |
|-----------------------------------------------|--------|
| Struktura projektu + Hilt DI                  | ✅     |
| Theme (Material 3, light + dark) + nawigacja  | ✅     |
| Brand font Poppins (3 weights, bundlowany)    | ✅     |
| Splash systemowy (Android 12+) + Compose      | ✅     |
| Adaptive launcher icon                        | ✅     |
| Themed icon (monochrome, Android 13+)         | ✅     |
| Modele i interfejsy repo                      | ✅     |
| Logowanie e-mail/hasło                        | ✅     |
| Logowanie Google (Credential Manager)         | ✅     |
| Reset hasła e-mailem                          | ✅     |
| Rejestracja + tworzenie dokumentu `users`     | ✅     |
| Wylogowanie                                   | ✅     |
| Home — hero, Top miejsca, Blisko Ciebie       | ✅     |
| Mapa Google Maps + customowe pinezki kategorii| ✅     |
| Filtry mapy (kategoria + najlepiej oceniane)  | ✅     |
| Bottom sheet preview pinezki + "Nawiguj"      | ✅     |
| Lista miejsc + filtr kategorii                | ✅     |
| Filtr udogodnień (4 quick + sheet z grupami)  | ✅     |
| Dodawanie miejsca (formularz + GPS + geocoder)| ✅     |
| Edycja miejsca (`AddPlace?placeId=`)          | ✅     |
| Usuwanie miejsca (z dialog potwierdzeniem)    | ✅     |
| Szczegóły miejsca + autor + udogodnienia      | ✅     |
| Snapshot listenery na places i reviews        | ✅     |
| Profil — nazwa / e-mail / wyloguj             | 🟡 — statystyki, avatar i odznaki TODO |
| Dodawanie opinii                              | ⏳ — `addReview` w repo zwraca `NotImplementedError` |
| Zgłaszanie opinii jako spam                   | ⏳ — `reportReviewAsSpam` j.w. |
| Ranking i odznaki                             | ⏳ — `RankingScreen` to czysty placeholder z `Text(...)` |
| Upload zdjęć do Firebase Storage              | ⏳ — Storage dep wpięte, ale brak logiki w kodzie |

## Kolejne kroki

1. **Opinie** — implementacja `FirestoreReviewRepository.addReview` i
   `reportReviewAsSpam` (transakcja: dopisanie review + aktualizacja
   `averageRating` / `reviewsCount` na dokumencie Place). UI dodawania
   opinii w `PlaceDetailsScreen`.
2. **Cloud Functions / agregaty** — przeniesienie utrzymywania
   `averageRating` i `reviewsCount` na backend (trigger na write
   w `reviews`), żeby klient nie polegał na transakcjach i nie miał
   race condition przy równoczesnych ocenach.
3. **Upload zdjęć** — wpiąć Firebase Storage w `AddPlaceScreen`
   (zdjęcia miejsca) i `addReview` (zdjęcia w opiniach). Wykorzystać
   uprawnienia `CAMERA` i `READ_MEDIA_IMAGES` które są już w manifeście.
4. **Ranking** — TOP 10 miejsc (już mamy `getTopPlaces`) + ranking
   użytkowników (po `placesAddedCount` / `reviewsCount` w `users`)
   + odznaki (odkrywca, recenzent, ekspert rodzinny).
5. **Profil** — avatar (z `users.avatarUrl`, edycja przez Storage),
   statystyki, odznaki.
6. **Themed icon (vector)** — obecny `ic_launcher_monochrome` jest PNG-iem;
   docelowo lepiej mieć wersję wektorową single-path, by Android mógł
   sensownie zastosować dynamic color overlay.
7. **Snapshot listener dla "Top miejsc"** — `HomeViewModel` używa one-shot
   `getTopPlaces`; po wdrożeniu Cloud Functions z denormalizowaną kolekcją
   `top_places` można podmienić na listener.
8. **Geo zapytania** — `getPlacesNear` w repo nadal pobiera wszystkie
   miejsca i sortuje klient-side haversinem; przy rosnącej bazie warto
   przepisać na geohash / GeoFirestore.
9. Opcjonalnie: deep linking, push notifications, refinement dark mode.
