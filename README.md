# Aplikacja mobilna: Miejsca przyjazne dzieciom

## Opis projektu

Aplikacja mobilna dla rodziców, umożliwiająca szybkie wyszukiwanie i ocenianie miejsc przyjaznych małym dzieciom w dużych miastach (place zabaw, kawiarnie i restauracje z kącikiem dziecięcym, sale zabaw itp.).

Użytkownicy po zalogowaniu mogą:

* dodawać nowe miejsca,
* edytować i usuwać własne wpisy,
* komentować i oceniać miejsca dodane przez innych.

Projekt skupia się na prostocie, geolokalizacji i realnej użyteczności w codziennym życiu rodzica.

---

## Zakres MVP

### 1. Uwierzytelnianie

* Rejestracja i logowanie użytkowników (e‑mail/hasło).
* Opcjonalne logowanie federacyjne (Google).

### 2. Wyszukiwanie i przeglądanie miejsc

* Lista miejsc z filtrowaniem:

  * typ miejsca,
  * miasto,
  * odległość,
  * udogodnienia.
* Widok szczegółów miejsca:

  * opis,
  * adres,
  * godziny otwarcia,
  * udogodnienia,
  * średnia ocena i liczba ocen.
* Widok lista + mapa.
* Wyszukiwanie miejsc w pobliżu użytkownika.

### 3. Dodawanie i edycja miejsc

* Dodawanie nowych miejsc przez zalogowanych użytkowników.
* Edycja i usuwanie wyłącznie przez autora wpisu.
* Walidacja danych wejściowych (nazwa, adres, typ, koordynaty).

### 4. Komentarze i oceny

* Dodawanie komentarzy przez zalogowanych użytkowników.
* System ocen w skali 1–5.
* Automatyczne wyliczanie średniej oceny.
* Podstawowa moderacja:

  * zgłaszanie nadużyć,
  * możliwość ukrycia komentarzy.

### 5. Geolokalizacja (element obowiązkowy MVP)

* Pobieranie lokalizacji użytkownika (GPS).
* Sortowanie miejsc po odległości.
* Wyświetlanie miejsc na mapie.

---

## Stos technologiczny (Android)

### UI

* **Jetpack Compose** – deklaratywne UI.
* **Material 3** – spójny i nowoczesny design.

### Architektura

* **MVVM** + warstwa Repozytorium.
* **Hilt** – wstrzykiwanie zależności.
* **Kotlin Coroutines + Flow** – asynchroniczność i reaktywność.

### Dane

* **Room** – cache offline.
* **Retrofit + OkHttp** – komunikacja z API.
* **Kotlinx Serialization** lub **Moshi** – serializacja danych.

### Backend / usługi zewnętrzne

* **Supabase** (rekomendowane):

  * open‑source,
  * darmowy tier,
  * Auth + Postgres + Storage.

Alternatywy:

* Firebase (szybki start, brak open‑source),
* własny backend: **Ktor** lub **Spring Boot** + **PostgreSQL**.

### Mapy

* **Google Maps SDK** / **Maps Compose**.

---

## Model danych (przykład)

```kotlin
data class Place(
    val id: String,
    val name: String,
    val type: PlaceType,
    val city: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val description: String,
    val amenities: List<String>,
    val authorId: String,
    val createdAt: Long,
    val ratingAvg: Double,
    val ratingCount: Int
)

data class Comment(
    val id: String,
    val placeId: String,
    val authorId: String,
    val text: String,
    val createdAt: Long,
    val rating: Int
)

enum class PlaceType {
    PLAYGROUND,
    CAFE,
    RESTAURANT,
    INDOOR_PLAYGROUND
}
```

---

## Warstwa danych

### Repozytorium

```kotlin
interface PlacesRepository {
    fun observePlaces(): Flow<List<Place>>
    suspend fun addPlace(place: Place)
    suspend fun updatePlace(place: Place)
    suspend fun addComment(placeId: String, comment: Comment)
}
```

### ViewModel (przykład)

```kotlin
@HiltViewModel
class PlacesViewModel @Inject constructor(
    private val repo: PlacesRepository
) : ViewModel() {

    val places = repo.observePlaces()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    fun addPlace(place: Place) {
        viewModelScope.launch {
            repo.addPlace(place)
        }
    }
}
```

---

## UI (Jetpack Compose)

### Lista miejsc

```kotlin
@Composable
fun PlacesScreen(viewModel: PlacesViewModel) {
    val places by viewModel.places.collectAsState()

    LazyColumn {
        items(places) { place ->
            PlaceCard(place)
        }
    }
}

@Composable
fun PlaceCard(place: Place) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text(place.name, style = MaterialTheme.typography.titleMedium)
            Text(place.address)
            Text(place.type.name)
        }
    }
}
```

---

## Zasady dostępu i bezpieczeństwo

* Edycja i usuwanie miejsca tylko przez autora wpisu:
  `authorId == request.auth.uid`.
* Komentarze dostępne dla każdego zalogowanego użytkownika.
* Jedna ocena użytkownika na jedno miejsce.
* Możliwość zgłaszania nadużyć.

---

## Plan wdrożenia

### Etap 1: Decyzje MVP

* Mapa i geolokalizacja: **tak**.
* Komentarze i oceny: **tak**.
* Zdjęcia: **nie** (po MVP).
* Backend: **Supabase** lub własne API.

### Etap 2: Szkielet aplikacji

* Nowy projekt Android (Jetpack Compose).
* Konfiguracja Hilt, Coroutines, Material 3.
* Integracja map.

### Etap 3: API i modele

* Definicja modeli danych.
* Endpointy:

  * `GET /places`
  * `POST /places`
  * `PATCH /places/{id}`
  * `GET /places/{id}/comments`
  * `POST /places/{id}/comments`

### Etap 4: Ekrany MVP

* Lista miejsc + filtry.
* Szczegóły miejsca.
* Mapa z pinami.
* Formularz dodawania miejsca.

### Etap 5: Autoryzacja

* Logowanie użytkowników.
* Reguły dostępu do edycji i ocen.

### Etap 6: Testy i jakość

* Testy jednostkowe repozytoriów i ViewModeli.
* Walidacja formularzy.
* Podstawowe testy UI.

---

## Status projektu

MVP – w fazie projektowania / implementacji.

Projekt nadaje się jako:

* aplikacja produkcyjna,
* projekt portfolio,
* baza pod dalszą rozbudowę (zdjęcia, ulubione miejsca, powiadomienia).
