# kidZone

> Mapa miejsc przyjaznych dzieciom — aplikacja mobilna Android.

kidZone to społecznościowa aplikacja dla rodziców i opiekunów. Pozwala
dodawać i oceniać miejsca przyjazne dzieciom (place zabaw, restauracje
z kącikiem dla dzieci, sale zabaw, parki, kawiarnie rodzinne), wyszukiwać
je na mapie, filtrować po kategorii i sprawdzać najlepsze pozycje
w okolicy.

To repozytorium zawiera **minimalny szkielet projektu** — kompiluje się,
uruchamia, pokazuje nawigację między ekranami; logika biznesowa jest
zaznaczona jako `TODO` i będzie wypełniana w kolejnych iteracjach.

## Stack

- **Kotlin 2.0.20** + **Jetpack Compose** (Material 3, BOM 2024.09.03)
- **Hilt 2.52** (DI)
- **Firebase** (Auth, Firestore, Storage) – BOM 33.4.0
- **Google Maps** SDK + `maps-compose`
- **Coil** (obrazy), **Retrofit + OkHttp** (na przyszłość, dla Opcji 2 backendu),
  **Room** (cache offline)
- **Navigation Compose**, **Coroutines**
- Architektura: **MVVM + Clean Architecture** (warstwy `data` / `domain` / `presentation`)

## Struktura projektu

```
app/src/main/java/com/kidzone
├── KidZoneApplication.kt    # @HiltAndroidApp
├── MainActivity.kt          # @AndroidEntryPoint, host Compose
│
├── data
│   ├── remote
│   │   ├── FirestoreCollections.kt
│   │   └── dto/             # UserDto, PlaceDto, ReviewDto
│   └── repository/          # Firebase implementacje repozytoriów
│
├── domain
│   ├── model/               # User, Place, Review, Photo, PlaceCategory, Amenity
│   └── repository/          # interfejsy: AuthRepository, PlaceRepository, ReviewRepository
│
├── presentation
│   ├── splash/              # SplashScreen + SplashViewModel
│   ├── auth/                # LoginScreen, RegisterScreen
│   ├── main/                # MainScreen (shell z bottom navigation)
│   ├── home/                # HomeScreen
│   ├── map/                 # MapScreen
│   ├── place
│   │   ├── list/            # PlaceListScreen
│   │   ├── details/         # PlaceDetailsScreen
│   │   └── add/             # AddPlaceScreen
│   ├── profile/             # ProfileScreen
│   └── ranking/             # RankingScreen
│
├── ui/theme/                # Color, Type, Theme (paleta marki)
├── navigation/              # Routes (sealed class) + NavGraph
├── di/                      # FirebaseModule, RepositoryModule
└── utils/                   # OpResult sealed interface
```

## Mapa schematu bazy

- Kolekcja `users` ↔ `domain.model.User` ↔ `data.remote.dto.UserDto`
- Kolekcja `places` ↔ `domain.model.Place` ↔ `data.remote.dto.PlaceDto`
- Kolekcja `reviews` ↔ `domain.model.Review` ↔ `data.remote.dto.ReviewDto`
- (`photos` zarządzane razem z `places` przez listę URL-i z Firebase Storage)

## Uruchomienie lokalne

### 1. Wymagania

- Android Studio Koala (lub nowsze) z JDK 17
- Gradle 8.9 (wrapper – wystarczy `./gradlew` po zainicjowaniu)
- Konto Firebase + projekt z włączonymi: Authentication, Firestore, Storage
- Klucz Google Maps (Maps SDK for Android)

### 2. Wygenerowanie wrappera Gradle

W repo nie ma `gradlew`/`gradle-wrapper.jar` — wygeneruj je raz:

```bash
gradle wrapper --gradle-version 8.9
```

Alternatywnie zaimportuj projekt do Android Studio – IDE doda wrapper za Ciebie.

### 3. Konfiguracja Firebase

1. W konsoli Firebase utwórz projekt.
2. Dodaj aplikację Android z `applicationId = com.kidzone`.
3. Pobierz `google-services.json` i wrzuć do `app/google-services.json`.
   (Przykładowa struktura jest w `app/google-services.json.template`).
4. Włącz: Authentication (e-mail/hasło, Google), Cloud Firestore, Storage.

### 4. Konfiguracja klucza Google Maps

W pliku `local.properties` (nie committuj) dodaj:

```
MAPS_API_KEY=AIzaSy...twoj_klucz
```

Klucz jest podstawiany do `AndroidManifest.xml` jako `${MAPS_API_KEY}`.

### 5. Build

```bash
./gradlew assembleDebug
```

## Status MVP

| Funkcja                       | Status |
|-------------------------------|--------|
| Struktura projektu i DI       | ✅     |
| Theme + nawigacja             | ✅     |
| Modele i interfejsy repo      | ✅     |
| Logowanie e-mail/Google       | ⏳ TODO |
| Mapa Google Maps + pinezki    | ⏳ TODO |
| Dodawanie miejsc              | ⏳ TODO |
| Oceny i komentarze            | ⏳ TODO |
| Lista miejsc + filtry         | ⏳ TODO |
| Ranking i odznaki             | ⏳ TODO |

## Kolejne kroki

1. Podłączyć logikę `FirebaseAuthRepository` (e-mail/hasło, Google Sign-In, reset).
2. Zaimplementować `FirestorePlaceRepository.observePlaces` na snapshot listenerach.
3. Zbudować `MapScreen` na `maps-compose` z pinezkami i filtrami.
4. Zbudować formularz `AddPlaceScreen` + upload zdjęć do Storage.
5. Dodać ViewModele dla list/detali i wpiąć je w ekrany.
