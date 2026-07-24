# Data Flow

Ostatnia aktualizacja: 2026-07-13

## Cel

Dokument opisuje przepływ danych w kidZone od akcji użytkownika, przez ViewModel i warstwę domenową, do Room, Firebase i z powrotem do UI.

## Odczyt danych

```text
User
  → Composable
  → ViewModel
  → Use Case
  → Repository Interface
  → Repository Implementation
  → Room / Firestore / Storage
  → Mapper
  → Domain Model
  → UiState
  → Compose UI
```

## Zapis danych

```text
User action
  → Composable event
  → ViewModel action
  → Validation
  → Use Case
  → Repository
  → Local pending state
  → Firestore / Storage
  → Sync result
  → UiState / UI event
```

## UiState

Złożony ekran powinien mieć spójny model stanu obejmujący odpowiednie warianty:

```text
Idle
Loading
Success
Empty
Error
Offline
PermissionRequired
```

Nie należy utrzymywać wielu nieskoordynowanych flag, gdy ekran ma rozbudowany lifecycle danych.

## UI

UI:

- emituje akcje użytkownika,
- obserwuje `StateFlow`,
- renderuje stan,
- nie wykonuje operacji na Firebase ani Room,
- nie mapuje wyjątków technicznych,
- nie decyduje o źródle danych.

## ViewModel

ViewModel:

- przyjmuje akcje z UI,
- uruchamia use case'y,
- mapuje wyniki na `UiState`,
- wystawia eventy jednorazowe, gdy są potrzebne,
- nie zna strategii cache ani szczegółów Firebase.

## Repository

Repository:

- ukrywa źródło danych,
- łączy Room i Firebase,
- realizuje cache i synchronizację,
- stosuje limity i paginację,
- mapuje DTO i Entity na modele domenowe,
- zwraca kontrolowany wynik.

## Mappery

```text
Firestore DTO → Domain Model
Room Entity → Domain Model
Domain Model → DTO / Entity
```

Mapper nie zawiera logiki UI.

## Cache i offline-first

Preferowany przepływ dla ekranów listowych:

```text
Room cache → UI
Firebase snapshot → Room update → UI refresh
```

Zasady:

- repository decyduje o źródle danych,
- cache ma jawny czas i sposób odświeżania,
- pull-to-refresh wymusza odświeżenie,
- przełączanie zakładek nie pobiera całej bazy,
- operacje oczekujące mają status synchronizacji,
- retry nie tworzy duplikatów.

## Lista miejsc

- pierwsza paczka danych zamiast pełnej kolekcji,
- paginacja i limity,
- filtry oraz sortowanie poza Composable,
- brak osobnego pobierania opinii dla każdej karty,
- użycie pól agregowanych, takich jak `ratingAverage` i `reviewsCount`.

## Mapa

- pobieranie danych dla bounds lub promienia,
- limit markerów,
- clustering,
- debounce lub throttle ruchu mapy,
- brak pobierania całej kolekcji,
- lista jako fallback.

## Ranking

- gotowe pola rankingowe,
- limit wyników,
- brak liczenia średnich przez pobieranie wszystkich opinii,
- obsługa pustego zbioru.

## Obsługa błędów

```text
Firebase / Room / Android exception
  → Error Mapper
  → Domain Result
  → UiText / UiState
  → komunikat dla użytkownika
```

Surowe wyjątki nie trafiają do UI ani telemetrycznych pól zawierających dane użytkownika.

## Uprawnienia i funkcje platformowe

Przepływ funkcji platformowej powinien być współdzielony:

```text
UI action
  → shared permission handler
  → system dialog albo app settings
  → result
  → ViewModel / UI state refresh
```

Dotyczy to lokalizacji, kamery i powiadomień. Kolejne odmowy nie mogą pozostawiać martwej akcji.

## Checklista

- [ ] przepływ jest jednokierunkowy,
- [ ] UI nie zna Firebase ani Room,
- [ ] ViewModel nie zwraca DTO,
- [ ] repository ukrywa źródła danych,
- [ ] mappery oddzielają modele,
- [ ] błędy są mapowane,
- [ ] cache i retry są kontrolowane,
- [ ] listy i mapy mają limity,
- [ ] wspólne flow uprawnień nie jest duplikowane.
