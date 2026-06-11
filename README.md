# kidZone 🐻

Społecznościowa aplikacja mobilna dla rodziców — odkrywaj, dodawaj i oceniaj miejsca przyjazne dzieciom.

## 📱 Funkcje

### Dla użytkowników:
- 🗺️ **Interaktywna Mapa**: Zaawansowany widok Google Maps z wyszukiwaniem po nazwie, filtrowaniem kategorii i autorskim systemem pinezek.
- 🔍 **Globalne Wyszukiwanie**: Hybrydowe wyszukiwanie prefixowe (Firestore `startAt/endAt`) z fallbackiem offline (Room `LIKE`). Optymalizacja dzięki 300ms debounce i deduplikacji zapytań.
- ✨ **Shared Element Transitions**: Płynne przechodzenie ikon kategorii oraz avatarów użytkowników (Ranking → Profil) między ekranami.
- 👤 **Publiczne Profile**: Możliwość przeglądania osiągnięć, statystyk i odznak innych użytkowników.
- 🏆 **System Grywalizacji**: Rankingi TOP 100 miejsc i użytkowników. Dynamicznie przyznawane odznaki z chronologicznym śledzeniem zdobycia.
- ⚡ **Wydajne Ładowanie**: Skeleton Loaders (Shimmer) z backupem danych (`lastPlaces`) zapobiegającym migotaniu UI podczas przełączania filtrów.
- 📍 **Dodawanie miejsc**: Formularz z inteligentnym Reverse Geocodingiem, wykrywaniem duplikatów w promieniu 100m i kategoryzacją udogodnień.
- ⭐ **Opinie i Media**: System recenzji z wielokrotnym przesyłaniem zdjęć, kompresją WebP i detekcją duplikatów MD5 na poziomie bitowym.
- 🚨 **Bezpieczeństwo**: System zgłaszania naruszeń i weryfikacji zmian przez społeczność.

### Panel administracyjny (React):
- 📊 Dashboard ze statystykami i zarządzaniem zgłoszeniami.
- 📝 Moderacja treści i zatwierdzanie propozycji zmian danych.
- 👥 Zarządzanie użytkownikami (blokowanie, resetowanie danych).

## 🛠️ Tech Stack

### Android:
- **Kotlin + Jetpack Compose**: Deklaratywne UI z wykorzystaniem Material 3.
- **Shared Transition API**: Wykorzystanie eksperymentalnych API dla natywnych odczuć nawigacji.
- **Hilt (Dependency Injection)**: Skalowalne zarządzanie zależnościami.
- **Firebase**: Auth, Firestore, Storage, Cloud Functions, FCM.
- **Room**: Lokalny cache dla strategii offline-first.
- **Coil**: Optymalne ładowanie obrazów z pamięci i sieci.

### Backend (Cloud Functions):
- **Node.js/TypeScript**: Triggery bazy danych (onWrite/onUpdate) do agregacji statystyk, przeliczania rankingów i czyszczenia osieroconych danych.

## 📈 Rozwiązania Architektoniczne

- **State Persistence**: ViewModels przechowują ostatnio wczytane listy podczas stanów `Loading`, co pozwala na "Instant Memory Rendering" przy zmianie filtrów.
- **Debounced Flow**: Zastosowanie `flatMapLatest` w połączeniu z `debounce` drastycznie redukuje liczbę zapytań do Firestore przy szybkim pisaniu na klawiaturze.
- **Unified Components**: Centralny `CategoryIcon` jako "Single Source of Truth" dla stylistyki kategorii w całej aplikacji, kluczowy dla stabilności animacji Shared Elements.
- **Reliable Testing**: Zestaw ponad 210 testów jednostkowych (JUnit 5 + MockK) weryfikujących logikę biznesową, timingi Flow oraz stany UI.

## 📁 Struktura projektu

```
├── app/                    # Android app (Kotlin/Compose)
│   ├── src/main/java/com/kidzone/
│   │   ├── data/           # Repozytoria, Room DAO, Firebase Logic
│   │   ├── domain/         # Interfejsy i Modele biznesowe
│   │   ├── presentation/   # UI (Screens, ViewModels, Components)
│   │   └── utils/          # Normalizacja tekstu, MD5, Image Tools
├── admin-panel/            # Panel administratora (React 19 + MUI 6)
├── functions/              # Logika backendowa (TypeScript)
```

## 🚀 Setup

1. Skopiuj `google-services.json.template` do `app/google-services.json` i uzupełnij klucze.
2. W `local.properties` dodaj: `MAPS_API_KEY=twoj_klucz`.
3. Build & Run: `./gradlew installDebug`.

## 📄 Licencja

Projekt prywatny - kidZone 🐻.
