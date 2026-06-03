# kidZone

> Mapa miejsc przyjaznych dzieciom — aplikacja mobilna Android.

kidZone to społecznościowa aplikacja dla rodziców i opiekunów. Pozwala
dodawać i oceniać miejsca przyjazne dzieciom (place zabaw, restauracje
z kącikiem dla dzieci, sale zabaw, parki, kawiarnie rodzinne), wyszukiwać
je na mapie, filtrować po kategorii i udogodnieniach, sortować po
odległości / ocenach / dacie dodania, sprawdzać miejsca najwyżej
oceniane i te w okolicy. Po dodaniu min. 3 opinii do miejsca pojawia
się rozkład ocen w stylu Google Maps; pierwsza dziesiątka rankingu
dostaje plakietkę "TOP 100" z numerem pozycji.

MVP jest zaimplementowane end-to-end: Firebase Auth (e-mail + Google
przez Credential Manager), Firestore z snapshot listenerami, Google
Maps z customowymi pinezkami, formularz dodawania/edycji/usuwania
miejsc z GPS i reverse geocodingiem, pełen CRUD opinii (z transakcjami
agregującymi `averageRating` i `reviewsCount`), profil z avatarem,
zmianą hasła / e-maila, usunięciem konta, polityka prywatności RODO,
ranking użytkowników i miejsc, "Moje miejsca" / "Moje opinie".
Pozostałe TODO są w sekcji [Status MVP](#status-mvp).

## Stack

- **Kotlin 2.0.20** + **Jetpack Compose** (Material 3, BOM 2024.09.03)
- **Hilt 2.52** (DI) + KSP 2.0.20-1.0.25
- **Firebase** BOM 33.4.0 — Auth, Firestore, Storage
- **Google Maps** SDK 19.0.0 + `maps-compose` 4.4.1, `play-services-location` 21.3.0
- **Credential Manager** 1.3.0 + `googleid` 1.1.1 (nowoczesne Google Sign-In)
- **androidx.core:core-splashscreen** 1.0.1 (Splash Screen API Android 12+, backportowane)
- **Poppins** jako brand font (Google Font, OFL, bundlowany w `res/font/`, 3 weights: 400/600/700)
- **Coil 2.7.0** (obrazy + avatar), **Retrofit 2.11.0 + OkHttp** (na przyszłość), **Room 2.6.1** (offline cache miejsc — `PlaceEntity` + `PlaceDao` + `KidZoneDatabase`)
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
- **Login / Register** — gradient `#F5F8FB → #FFFFFF` (płynne przejście
  ze splasha), logo brandu w nagłówku, karta z formularzem, separator
  "lub", przycisk Google z mini-glyphem; pod polem hasła live checklist
  wymagań ([PasswordPolicy](app/src/main/java/com/kidzone/utils/PasswordPolicy.kt)).
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

## Polityka haseł

Zdefiniowana w [`utils/PasswordPolicy.kt`](app/src/main/java/com/kidzone/utils/PasswordPolicy.kt)
i egzekwowana w 3 miejscach (rejestracja, zmiana hasła w profilu, komunikat
`AuthException.WeakPassword`):

- minimum **8 znaków**,
- co najmniej **1 mała** litera,
- co najmniej **1 duża** litera,
- co najmniej **1 znak specjalny** (znak nie-literowy i nie-cyfrowy).

UI rejestracji i dialogu zmiany hasła pokazuje live checklist zbudowany
z `PasswordPolicy.evaluate(password)`.

## Unikalność loginu

`User.name` (publiczny nick widoczny w opiniach, miejscach, rankingu)
musi być unikalny **case-insensitive** w skali apki — „Adam" i „adam"
to ten sam login.

Mechanizm:
- `UserDto.nameLowercase` — pole pomocnicze ustawiane przy każdym save
  (`name.lowercase(pl_PL)`); pozwala na zapytanie
  `whereEqualTo("nameLowercase", X)` bez potrzeby natywnego collation.
- `FirebaseAuthRepository.isUsernameTaken` — **dwustopniowe sprawdzanie**:
  1. fast path po `nameLowercase` (po backfillu wszyscy userzy mają),
  2. legacy fallback do 500 doców bez wypełnionego `nameLowercase` z
     porównaniem `name` po stronie klienta — gwarantuje, że duplikat
     starego usera „Adam" zostanie wykryty, zanim wszyscy się przelogują.
- `ensureUserDoc` — przy każdym `signIn` uzupełnia `nameLowercase` jeśli
  legacy doc go nie ma; backfill „w tle" zmniejszający koszt fallbacku
  z czasem.
- `AuthException.UsernameAlreadyTaken` — typowany błąd; UI rejestracji
  i edycji profilu pokazuje „Ta nazwa użytkownika jest już zajęta".

Race condition (dwóch userów rejestrujących to samo imię równocześnie)
świadomie pomijamy dla MVP — przy małej skali ryzyko znikome,
alternatywą jest osobna kolekcja `usernames/{lowercase}` z transakcyjnym
zapisem.

## Odznaki

15 odznak pogrupowanych w 6 kategorii — wszystkie zdefiniowane w
[`presentation/common/UserBadges.kt`](app/src/main/java/com/kidzone/presentation/common/UserBadges.kt).
Każda odznaka ma unikalną ikonę z Material Icons Extended (świadomie
wybrane tak, by 24dp ikon-only były jednoznacznie odróżnialne).

| Kategoria | Odznaka | Próg | Ikona |
|---|---|---|---|
| Pierwsze kroki | Pierwszy ślad | 1 miejsce | `AddLocationAlt` |
| | Pierwsza opinia | 1 opinia | `ChatBubble` |
| Drabinka miejsc | Odkrywca | 5 miejsc | `Explore` |
| | Kartograf | 15 miejsc | `Map` |
| | Tropiciel | 30 miejsc | `Terrain` |
| Drabinka opinii | Recenzent | 10 opinii | `RateReview` |
| | Krytyk | 25 opinii | `Reviews` |
| | Wytrawny recenzent | 50 opinii | `Stars` |
| Wszechstronność | Filar społeczności | 5+ miejsc i 5+ opinii | `Groups` |
| | Ekspert rodzinny | 10+ miejsc i 20+ opinii | `Verified` |
| Ranking użytkowników | Brązowy lider | 3. miejsce w TOP 100 userów | `MilitaryTech` (bronze) |
| | Srebrny lider | 2. miejsce | `MilitaryTech` (silver) |
| | Złoty lider | 1. miejsce | `EmojiEvents` (gold trophy) |
| Ranking miejsc | Lokalny faworyt | twoje miejsce w TOP 3 | `Whatshot` |
| | Architekt zabawy | twoje miejsce na #1 | `WorkspacePremium` |

### Kontekst rankingowy

`User.computeBadges(context)` przyjmuje opcjonalny
[`BadgeContext`](app/src/main/java/com/kidzone/presentation/common/UserBadges.kt)
z `userRank` (1-based pozycja w TOP 100 userów) i `bestPlaceRank`
(najlepsza pozycja jakiegokolwiek miejsca usera w TOP 100 miejsc).
Bez kontekstu (default empty) liczone są tylko odznaki count-based.

Filtry aktywności rankingu (zob. `RankingViewModel`) — tylko userzy
z ≥1 miejscem lub opinią, miejsca z >0 opiniami i >0.0 średnią — są
spójne z definicją odznak rankingowych: jeśli widzisz siebie #1 w
rankingu, dostajesz „Złotego lidera".

### Powiadomienia + chronologia

- **Detekcja**: `ProfileViewModel` przy każdej emisji usera oblicza
  pełen zestaw odznak (z `BadgeContext`), porównuje z
  `seen_badges_<uid>` w SharedPreferences (per-device, lokalne
  „czy pokazałem dialog?"). Nowe → kolejka `pendingNewBadges`
  → `BadgeEarnedDialog` (gratulacyjny).
- **Chronologia globalna**: nowe pole `UserDto.badgeEarnedAt:
  Map<String, Long>` w Firestore. `AuthRepository.recordBadgesEarned`
  zapisuje timestampy **first-write-wins** (dot-notation merge,
  żeby drugie urządzenie tego samego usera nie nadpisało). Dzięki
  temu na karcie usera w rankingu odznaki sortują się chronologicznie
  na każdym kliencie tak samo (`chronologicalOrder` z fallback na
  `enum.ordinal` dla legacy timestampów).

### Wyświetlanie

- **Profil** (sekcja „Odznaki") — pokazuje **tylko zdobyte** jako chipy
  z labelem (`BadgesRow`). Po prawej ikona „?" → `BadgesInfoDialog`
  (scrollowalny) z listą wszystkich odznak i progami; zdobyte
  podświetlone kolorem.
- **Ranking — karta usera** — `BadgesIconRow`: kompaktowe okrągłe
  kafelki 24dp (alpha 0.18 tła), bez tekstu, sortowane chronologicznie.
  `FlowRow` zawija na 2 rzędy przy 15 odznakach na typowym telefonie.
  Odznaki rankingowe (LEADER_*, PLACE_*) precomputowane w VM
  (`RankingViewModel.userBadges: Map<uid, List<UserBadge>>`), żeby
  karta usera w rankingu pokazywała te same odznaki co user widzi
  na własnym profilu.

## Normalizacja tekstów

[`utils/TextNormalization.kt`](app/src/main/java/com/kidzone/utils/TextNormalization.kt)
trzyma w jednym miejscu wspólne reguły kapitalizacji dla pól wprowadzanych
przez użytkownika (locale `pl_PL`, żeby polskie znaki działały):

- `toTitleCase` — Title Case dla nazwy miejsca i adresu (klawiatura
  używa `KeyboardCapitalization.Words`, normalizacja dzieje się przy save).
- `toSentenceCase` — Sentence case dla opisu (klawiatura używa
  `KeyboardCapitalization.Sentences`).

## Struktura projektu

```
app/src/main/java/com/kidzone
├── KidZoneApplication.kt    # @HiltAndroidApp
├── MainActivity.kt          # @AndroidEntryPoint, installSplashScreen + Compose
│
├── data
│   ├── remote
│   │   ├── FirestoreCollections.kt    # users, places, reviews, photos, place_reports, review_reports, place_change_requests
│   │   └── dto/                        # PlaceDto, ReviewDto, UserDto
│   ├── local
│   │   ├── KidZoneDatabase.kt         # Room DB v1 (fallbackToDestructiveMigration)
│   │   ├── PlaceDao.kt                # observe / query / upsert / delete
│   │   └── PlaceEntity.kt             # Room entity, mapowanie 1:1 z Place
│   └── repository
│       ├── FirebaseAuthRepository.kt   # e-mail + Google + reset, mapowanie błędów,
│       │                               # zmiana hasła / e-maila, deleteAccount,
│       │                               # uploadAvatar (Storage)
│       ├── FirestorePlaceRepository.kt # observe (snapshot) + Room offline cache (channelFlow),
│       │                               # top, near, add/update/delete (z timeoutem +
│       │                               # atomic increment user.placesAddedCount +
│       │                               # cache persist/fallback)
│       └── FirestoreReviewRepository.kt# observeReviewsForPlace + observeReviewsByUser,
│                                       # addReview / updateReview / deleteReview
│                                       # (transakcje agregujące averageRating + reviewsCount),
│                                       # reportReviewAsSpam (zapis do review_reports)
│
├── domain
│   ├── model/               # User, Place, Review, Photo, PlaceCategory, Amenity
│   └── repository/          # interfejsy: AuthRepository, PlaceRepository, ReviewRepository
│
├── presentation
│   ├── splash/              # SplashScreen + SplashViewModel
│   ├── auth
│   │   ├── LoginScreen.kt        # gradient, karta, e-mail + Google + reset
│   │   ├── LoginViewModel.kt
│   │   ├── RegisterScreen.kt     # nazwa + e-mail + hasło z live checklist
│   │   ├── RegisterViewModel.kt
│   │   └── GoogleSignInLauncher.kt   # Credential Manager + GetGoogleIdOption
│   ├── main/MainScreen.kt   # shell z bottom navigation + FAB "+"
│   ├── home
│   │   ├── HomeScreen.kt         # hero (zaokrąglony, węższy napis), CTA mapa,
│   │   │                         # Top 20, Blisko Ciebie 20
│   │   └── HomeViewModel.kt      # getTopPlaces(20) + haversine sort dla nearby (20),
│   │                             # komunikat timeout lokalizacji
│   ├── map
│   │   ├── MapScreen.kt          # GoogleMap + MarkerComposable + filtry +
│   │   │                         # bottom sheet pinezki
│   │   └── MapViewModel.kt       # observePlaces + flatMapLatest po kategorii,
│   │                             # filtry: topRatedOnly, addedByMeOnly
│   ├── place
│   │   ├── list
│   │   │   ├── PlaceListScreen.kt        # LazyColumn + chipy quick + button "Filtry"
│   │   │   │                             # + "Sortuj: ..." dropdown, banner zachęty
│   │   │   │                             # do włączenia lokalizacji, dystans na karcie
│   │   │   ├── PlaceListViewModel.kt     # observePlaces + filtr udogodnień AND
│   │   │   │                             # + 5 sort modes + cap LIST_LIMIT=100
│   │   │   └── AmenityFilterSheet.kt     # grupowane sekcje, auto-expand po kategorii
│   │   ├── details
│   │   │   ├── PlaceDetailsScreen.kt     # główna karta + udogodnienia + opinie
│   │   │   │                             # + plakietka "TOP 100" dla top 10 + edit/delete
│   │   │   │                             # dla ownera + "Dodano przez Ciebie"
│   │   │   ├── PlaceDetailsViewModel.kt  # getPlace + observeReviews + delete + autor
│   │   │   │                             # + topRank wyliczany z getTopPlaces(100)
│   │   │   └── AddReviewSheet.kt         # rating 1-5 + komentarz, tryb add/edit
│   │   ├── add
│   │   │   ├── AddPlaceScreen.kt         # formularz create/edit + GPS + KeyboardCapitalization
│   │   │   ├── AddPlaceViewModel.kt      # tryb create + edit (placeId), prune amenities
│   │   │   │                             # po zmianie kategorii, normalizacja capitalization save
│   │   │   └── LocationHelper.kt         # FusedLocation z timeoutem 15s + Geocoder
│   │   │                                 # (Android 13+ async API)
│   │   └── myplaces
│   │       ├── MyPlacesScreen.kt         # lista miejsc dodanych przez zalogowanego usera
│   │       └── MyPlacesViewModel.kt      # observePlacesByOwner z snapshot listenerem
│   ├── review/myreviews
│   │   ├── MyReviewsScreen.kt   # lista opinii zalogowanego usera
│   │   └── MyReviewsViewModel.kt# observeReviewsByUser
│   ├── profile
│   │   ├── ProfileScreen.kt          # avatar + nazwa + statystyki + odznaki +
│   │   │                             # sekcje: Moje miejsca / Moje opinie /
│   │   │                             # Konto i bezpieczeństwo / Polityka / Wyloguj
│   │   ├── ProfileViewModel.kt
│   │   ├── EditProfileSheet.kt       # nazwa / firstName / lastName / avatar
│   │   ├── ChangePasswordDialog.kt   # 3 pola + live PasswordPolicy checklist
│   │   ├── ChangeEmailDialog.kt      # verifyBeforeUpdateEmail z linkiem
│   │   ├── DeleteAccountDialog.kt    # potwierdzenie + reauth hasłem
│   │   └── PrivacyPolicyDialog.kt    # treść RODO + admin / kontakt z AppConfig
│   ├── ranking
│   │   ├── RankingScreen.kt     # 2 zakładki: TOP miejsc / TOP użytkowników;
│   │   │                        # karta usera z BadgesIconRow (ikon-only,
│   │   │                        # chronologicznie, FlowRow ~2 rzędy)
│   │   └── RankingViewModel.kt  # one-shot fetch + filtry aktywności +
│   │                            # precomputed userBadges per uid (z BadgeContext)
│   └── common
│       ├── CategoryStyle.kt     # ikona + kolor per PlaceCategory
│       ├── NetworkObserver.kt   # Flow<NetworkStatus> via ConnectivityManager
│       ├── StatusBanners.kt     # NoInternetBanner, GpsDisabledBanner,
│       │                        # rememberNetworkStatus(), rememberLocationServiceEnabled()
│       └── UserBadges.kt        # 15 odznak (6 kategorii) + BadgeContext
│                                # + BadgesRow / BadgesIconRow / chronologicalOrder
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
│   ├── DatabaseModule.kt    # @Provides Room DB + PlaceDao
│   ├── FirebaseModule.kt    # @Provides FirebaseAuth/Firestore/Storage
│   └── RepositoryModule.kt  # @Binds dla 3 repo
│
└── utils
    ├── Result.kt            # OpResult<T>: sealed Success/Failure + map
    ├── AuthException.kt     # typowane błędy auth (UserNotFound, WeakPassword, ...)
    ├── PasswordPolicy.kt    # min 8 + 1 mała + 1 duża + 1 specjalny
    ├── TextNormalization.kt # toTitleCase + toSentenceCase (locale pl_PL)
    └── AppConfig.kt         # ADMINISTRATOR_NAME, PRIVACY_CONTACT_EMAIL itp.
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
  - liczniki `placesAddedCount` / `reviewsCount` aktualizowane atomic
    `FieldValue.increment` przy dodaniu miejsca / opinii,
  - `nameLowercase` — pomocnicze pole dla case-insensitive sprawdzania
    unikalności loginu (zob. [Unikalność loginu](#unikalność-loginu)),
  - `badgeEarnedAt: Map<String, Long>` — timestampy zdobycia odznak
    (first-write-wins, do chronologicznego sortu na karcie rankingu).
- Kolekcja `places` ↔ `domain.model.Place` ↔ `data.remote.dto.PlaceDto`
  (pola `averageRating` / `reviewsCount` aktualizowane transakcyjnie
  przez `FirestoreReviewRepository`)
- Kolekcja `reviews` ↔ `domain.model.Review` ↔ `data.remote.dto.ReviewDto`
  (top-level, z polem `placeId`; filtrowanie `whereEqualTo("placeId", ...)`
  bez wymagania composite indexa — `reportedAsSpam=false` filtrujemy
  po stronie klienta)
- Stała `FirestoreCollections.PHOTOS = "photos"` jest zarezerwowana, ale
  na dziś `photoUrls` jest trzymane jako lista URL-i bezpośrednio na
  dokumencie miejsca (z Firebase Storage); avatary userów lecą do
  `avatars/{uid}/avatar.jpg`.

Reguły bezpieczeństwa są w `firestore.rules` i `storage.rules` w roocie repo.

## Kluczowe decyzje architektoniczne

- **MVVM + Clean** — interfejsy repo w `domain/repository`, implementacje
  w `data/repository`, ViewModele tylko widzą interfejsy. Hilt wstrzykuje
  konkretne `Firestore...Repository` przez `RepositoryModule`.
- **Snapshot listenery wszędzie tam, gdzie jest sens** — lista miejsc,
  mapa, opinie, "Moje miejsca", "Moje opinie", profil. Zmiany pojawiają
  się live bez pull-to-refresh.
- **One-shot fetch** dla rankingu i topu miejsc (Home, Ranking,
  PlaceDetails.topRank) — nie potrzebują real-time, redukują liczbę
  aktywnych listenerów.
- **`OpResult<T>` zamiast wyjątków** w warstwie repo — UI dostaje typowany
  Success/Failure + komunikaty `AuthException`. Wyjątki rzucamy tylko
  w utility (np. `LocationHelper.fetchCurrentLocation`), gdzie wzorzec
  Result byłby przesadą.
- **Transakcje agregatów** — `averageRating` i `reviewsCount` na
  `places` aktualizowane są w transakcji razem z dodaniem opinii;
  `placesAddedCount` na `users` przez `FieldValue.increment`. Przy
  równoczesnych zapisach z innych klientów może wystąpić chwilowy
  rozjazd o 1 — akceptowalne dla MVP.
- **`KeyboardCapitalization` + `TextNormalization`** — klawiatura sama
  podpowiada Title/Sentence case przy wpisywaniu, a normalizacja przy
  save sprząta reszki autocorrect / ALL CAPS.
- **TOP 100 / TOP 10** — `PlaceDetailsViewModel.loadTopRank` pobiera
  top 100 wg `averageRating` i wystawia plakietkę tylko dla pierwszej
  dziesiątki. Pula jest większa niż wyróżnienie, żeby rosnąca baza
  miejsc nie deaktywowała plakietki przy mikroskopijnych różnicach
  ocen w pobliżu progu.

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
6. Wrzuć reguły bezpieczeństwa: Firestore z `firestore.rules`, Storage
   z `storage.rules`. Bez tego Storage `uploadAvatar` zwróci 403 i
   "Konto i bezpieczeństwo" → zmiana avatara nie zadziała.

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
  na mapie, GPS w `AddPlaceScreen`, sekcje "Blisko Ciebie" na home, sortowanie
  "Najbliższe" na liście miejsc
- `CAMERA`, `READ_MEDIA_IMAGES` — pod uploady zdjęć (avatar już to wykorzystuje
  przez photo picker; zdjęcia miejsc / opinii — TODO)

Prośby o uprawnienia są wykonywane in-app: `MapScreen` automatycznie
przy pierwszym wejściu, `AddPlaceScreen` przy kliknięciu "Pobierz lokalizację",
`HomeScreen` przez kartę "Włącz lokalizację" w sekcji "Blisko Ciebie",
`PlaceListScreen` przez banner "Włącz lokalizację, by sortować po odległości".

## Status MVP

| Funkcja                                              | Status |
|------------------------------------------------------|--------|
| Struktura projektu + Hilt DI                         | ✅     |
| Theme (Material 3, light + dark) + nawigacja         | ✅     |
| Brand font Poppins (3 weights, bundlowany)           | ✅     |
| Splash systemowy (Android 12+) + Compose             | ✅     |
| Adaptive launcher icon + themed (Android 13+)        | ✅     |
| Login screen redesign (gradient, karta, Google glyph)| ✅     |
| Polityka haseł 8+/Aa/specjalny + live checklist      | ✅     |
| Logowanie e-mail/hasło + Google (Credential Manager) | ✅     |
| Reset hasła e-mailem                                 | ✅     |
| Rejestracja + tworzenie dokumentu `users`            | ✅     |
| Wylogowanie                                          | ✅     |
| Home — hero, Top miejsca (20), Blisko Ciebie (20)    | ✅     |
| Mapa Google Maps + customowe pinezki kategorii       | ✅     |
| Filtry mapy: kategoria + Najlepiej / Dodane przez Ciebie | ✅ |
| Bottom sheet preview pinezki + "Nawiguj"             | ✅     |
| Lista miejsc — filtry, 5 trybów sortowania, max 100  | ✅     |
| Filtr udogodnień (4 quick + sheet z grupami)         | ✅     |
| Dodawanie miejsca (formularz + GPS + geocoder)       | ✅     |
| Timeout lokalizacji 15s + komunikat user-friendly    | ✅     |
| Normalizacja kapitalizacji nazwy / adresu / opisu    | ✅     |
| Edycja miejsca (`AddPlace?placeId=`)                 | ✅     |
| Usuwanie miejsca (z dialog potwierdzeniem)           | ✅     |
| Szczegóły miejsca + autor + udogodnienia             | ✅     |
| "Dodano przez Ciebie" dla zalogowanego właściciela   | ✅     |
| Plakietka "TOP 100" + numer pozycji dla top 10       | ✅     |
| Snapshot listenery na places i reviews               | ✅     |
| Dodawanie / edycja / usuwanie opinii (transakcje)    | ✅     |
| Wykres rozkładu ocen (Google Maps style)             | ✅     |
| Sortowanie listy opinii (4 tryby)                    | ✅     |
| Profil — avatar, nazwa, statystyki, odznaki          | ✅     |
| Profil — Moje miejsca, Moje opinie                   | ✅     |
| Konto: zmiana hasła + zmiana e-maila + delete        | ✅     |
| Polityka prywatności RODO (in-app)                   | ✅     |
| Unikalność loginu (case-insensitive + legacy fallback)| ✅    |
| 15 odznak (count + ranking) + dialog gratulacyjny    | ✅     |
| Chronologiczny sort odznak w rankingu (Firestore)    | ✅     |
| Ranking miejsc (TOP 100) + użytkowników              | ✅     |
| Upload avatara do Firebase Storage                   | ✅     |
| Custom MapMyLocationButton na mapie                  | ✅     |
| Deeplink "Zobacz na Google Maps" (oceny Google)      | ✅     |
| Filtry listy category-aware (sheet filtrów)          | ✅     |
| Banner GPS na ekranie Dodaj miejsce                  | ✅     |
| Wykrywanie duplikatów przy dodawaniu (200m)          | ✅     |
| Ranking: auto-refresh + pull-to-refresh              | ✅     |
| Zbiorczy dialog nowych odznak (scrollowalny)         | ✅     |
| Logo misia w TopAppBar obok "kidZone"                | ✅     |
| Udostępnij miejsce (Share intent)                    | ✅     |
| Zgłoś naruszenie (dialog + `place_reports`)          | ✅     |
| Zaproponuj zmianę danych miejsca (`place_change_requests`) | ✅ |
| Koryguj lokalizację GPS miejsca                      | ✅     |
| Cloud Functions: email admin po zgłoszeniu/zmianie   | ✅     |
| Sprawdzanie włączonej usługi GPS w systemie          | ✅     |
| Zgłaszanie opinii jako spam                          | ✅     |
| Room offline cache (miejsca — offline-first reads)   | ✅     |
| Banner "Brak internetu" na wszystkich zakładkach     | ✅     |
| Banner "GPS wyłączony" na Home/Map/List              | ✅     |
| Pull-to-refresh na Home/List/Profile/Ranking         | ✅     |
| Cloud Functions: email admin po zgłoszeniu opinii    | ✅     |
| Cloud Functions: email powitalny + email przy usunięciu konta | ✅ |
| Upload zdjęć miejsc / opinii do Storage              | ⏳ — Storage dep wpięte, brak UI |

## Cloud Functions (backend)

Folder `functions/` zawiera Cloud Functions (TypeScript, Firebase Functions v2)
triggerowane przez zapis/usunięcie dokumentu w Firestore:

| Trigger | Kolekcja | Działanie |
|---------|----------|-----------|
| `onPlaceReport` | `place_reports` | Email do admina: zgłoszenie naruszenia miejsca |
| `onReviewReport` | `review_reports` | Email do admina: zgłoszenie opinii jako spam (z treścią opinii, autorem, gwiazdkami) |
| `onPlaceChangeRequest` | `place_change_requests` | Email do admina: propozycja zmiany / korekty lokalizacji |
| `onUserCreated` | `users` (onCreate) | Email powitalny do usera + powiadomienie admina |
| `onUserDeleted` | `users` (onDelete) | Email pożegnalny do usera + powiadomienie admina |

Email zawiera: nazwę miejsca, dane zgłaszającego (imię, email, UID),
powód / proponowane zmiany (zmapowane na czytelne polskie etykiety) +
link do dokumentu w Firebase Console.

Konfiguracja credentials przez Firebase Secrets (`defineSecret`):
- `GMAIL_EMAIL` — adres z którego wychodzą powiadomienia
- `GMAIL_PASSWORD` — App Password (Google 2FA)
- `ADMIN_EMAIL` — adres docelowy powiadomień

Deploy: `cd functions && npm run build && cd .. && firebase deploy --only functions`

## Kolejne kroki

1. **Upload zdjęć miejsc i opinii** — analogicznie do `uploadAvatar`,
   w `AddPlaceScreen` i `AddReviewSheet`. Wykorzystać `READ_MEDIA_IMAGES`
   i `CAMERA`, które już są w manifeście. User może dodać zdjęcie do
   cudzego miejsca (zapamiętane do implementacji).
2. **Geo zapytania** — `getPlacesNear` w repo nadal pobiera wszystkie
   miejsca i sortuje klient-side haversinem; przy rosnącej bazie
   przepisać na geohash / GeoFirestore.
3. **Panel admina do akceptacji zmian** — propozycje zmian i korekty
   lokalizacji trafiają do `place_change_requests`, ale akceptacja
   wymaga ręcznej edycji w Firebase Console. Docelowo: prosty panel
   webowy lub Cloud Function z auto-akceptacją po N zgodnych zgłoszeniach.
4. **Themed icon (vector)** — obecny `ic_launcher_monochrome` jest PNG-iem;
   docelowo lepiej mieć wersję wektorową single-path.
5. **Usuwanie starych wpisów z Room cache** — brak TTL / polityki
   wygaszania; stale entries czyszczą się przy najbliższym sync
   z Firestore, ale przy dużej bazie warto dodać periodyczny gc.
6. Opcjonalnie: deep linking, push notifications, refinement dark mode,
   paginacja listy miejsc.
