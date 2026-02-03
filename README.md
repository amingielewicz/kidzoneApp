# Aplikacja mobilna: Miejsca przyjazne dzieciom

## Cel
Aplikacja pomaga rodzicom wyszukiwać miejsca przyjazne małym dzieciom (place zabaw, kawiarnie i restauracje z kącikiem dla dzieci, sale zabaw itp.) w dużych miastach. Użytkownicy po zalogowaniu mogą dodawać nowe miejsca, edytować własne wpisy oraz komentować miejsca innych użytkowników.

## Plan funkcjonalny (MVP)

### 1. Uwierzytelnianie
- Rejestracja i logowanie użytkowników.
- Opcjonalnie logowanie federacyjne (np. Google).

### 2. Wyszukiwanie i przeglądanie miejsc
- Lista miejsc z filtrowaniem (typ miejsca, miasto, odległość, udogodnienia).
- Widok szczegółów miejsca: opis, adres, godziny otwarcia, udogodnienia, średnia ocena.
- Widok mapy (lista + mapa) oraz wyszukiwanie w pobliżu.

### 3. Dodawanie i edycja miejsc
- Dodawanie nowych miejsc przez zalogowanych użytkowników.
- Edycja i usuwanie wyłącznie przez autora wpisu.
- Walidacja danych (np. nazwa, adres, typ, koordynaty).

### 4. Komentarze i oceny
- Dodawanie komentarzy przez zalogowanych użytkowników.
- Wyświetlanie komentarzy w szczegółach miejsca.
- System ocen (np. 1–5) z wyliczaną średnią.
- Podstawowa moderacja (zgłoszenia, ukrywanie komentarzy).

### 5. Geolokalizacja
- Wyszukiwanie miejsc w pobliżu na podstawie GPS.
- Sortowanie po odległości.
- Geolokalizacja jest częścią MVP.

## Sugerowany stos technologiczny (Android)

### UI
- **Jetpack Compose** — nowoczesny UI.
- **Material 3** — spójny design.

### Architektura
- **MVVM** z warstwą Repozytorium.
- **Hilt** — wstrzykiwanie zależności.
- **Kotlin Coroutines + Flow** — asynchroniczność i reaktywność.

### Dane
- **Room** — cache offline.
- **Retrofit + OkHttp** — komunikacja z API.
- **Kotlinx Serialization** lub **Moshi** — serializacja.

### Usługi zewnętrzne (opcjonalnie)
- **Firebase Authentication** — szybkie logowanie.
- **Google Maps SDK / Maps Compose** — mapy.
- **Firestore** (backend no‑code) **lub** własne API (np. Ktor/Spring).
  - Uwaga: Firebase oferuje bezpłatny plan startowy, ale nie jest projektem open‑source.
  - Jeśli wymagane jest rozwiązanie open‑source, rozważ Supabase (open‑source + darmowy tier) albo własny backend (np. Ktor/Spring) z bazą Postgres.

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

enum class PlaceType { PLAYGROUND, CAFE, RESTAURANT, INDOOR_PLAYGROUND }
```

## Przykładowa warstwa repozytorium

```kotlin
interface PlacesRepository {
    fun observePlaces(): Flow<List<Place>>
    suspend fun addPlace(place: Place)
    suspend fun updatePlace(place: Place)
    suspend fun addComment(placeId: String, comment: Comment)
}
```

## Przykładowy ViewModel

```kotlin
@HiltViewModel
class PlacesViewModel @Inject constructor(
    private val repo: PlacesRepository
) : ViewModel() {

    val places = repo.observePlaces()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addPlace(place: Place) {
        viewModelScope.launch { repo.addPlace(place) }
    }
}
```

## UI: lista miejsc (Compose)

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

## Zasady dostępu i bezpieczeństwo
- Edycja/usuwanie możliwe tylko przez autora wpisu (`authorId == request.auth.uid`).
- Komentarze może dodawać każdy zalogowany użytkownik.
- Oceny (1–5) przypisane do komentarza lub osobnej kolekcji (1 ocena na użytkownika i miejsce).
- Podstawowy system zgłaszania nadużyć.

## Etapy wdrożenia (propozycja)
1. **MVP UI**: lista + szczegóły miejsca + mapa.
2. **Logowanie**.
3. **Dodawanie/edycja miejsc**.
4. **Komentarze + oceny**.
5. **Geolokalizacja (MVP)**.
6. **Cache offline**.
7. **Moderacja i rozwój funkcji społecznościowych**.
