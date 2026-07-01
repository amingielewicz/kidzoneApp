# Data Flow

## Cel

<<<<<<< HEAD
Dokument opisuje przepływ danych w KidZone: od UI, przez ViewModel i warstwę domenową, aż do Room, Firestore, Storage oraz Google Maps.
=======
Dokument opisuje przepływ danych w aplikacji KidZone od interakcji użytkownika do źródła danych i z powrotem do UI.
>>>>>>> c171379 (docs: add data flow documentation)

## Standardowy przepływ odczytu

```text
<<<<<<< HEAD
Composable
  -> ViewModel
  -> UseCase
  -> Repository
  -> Local cache / Remote source
  -> UiState
  -> Composable
=======
User
  -> Composable
  -> ViewModel
  -> Use Case / Repository Interface
  -> Repository Implementation
  -> Firebase / Room
  -> Mapper
  -> Domain Model
  -> UI State
  -> Compose UI
>>>>>>> c171379 (docs: add data flow documentation)
```

## Standardowy przepływ zapisu

```text
User action
<<<<<<< HEAD
  -> ViewModel
  -> UseCase
  -> Repository
  -> Validation
  -> Local pending state
  -> Firestore / Storage
  -> Sync result
  -> UiState update
```

## UiState

Każdy ekran powinien mieć jawny stan:

```text
Idle
Loading
Success
Empty
Error
Offline
PermissionRequired
```

Nie mieszamy flag typu `isLoading`, `errorMessage`, `data` bez spójnego modelu stanu, jeśli ekran robi się złożony.

## Cache

Zasady:

- Repository decyduje, czy dane pochodzą z Room, Firestore czy pamięci.
- ViewModel nie powinien znać strategii cache.
- Cache powinien mieć jasny moment odświeżania.
- Pull-to-refresh wymusza odświeżenie.
- Przełączenie zakładki nie powinno automatycznie pobierać całej bazy.

## Lista miejsc

Lista miejsc powinna:

- ładować pierwszą paczkę danych,
- używać paginacji,
- wspierać filtry i sortowanie poza Composable,
- nie pobierać opinii dla każdej karty,
- korzystać z gotowych pól `ratingAverage` i `reviewsCount`.

## Mapa

Mapa powinna:

- pobierać dane dla bounds albo promienia,
- mieć limit markerów,
- mieć debounce/throttle dla ruchu mapy,
- nie pobierać całej kolekcji miejsc,
- mieć fallback w postaci listy.

## Ranking

Ranking powinien:

- korzystać z gotowych pól rankingowych,
- nie liczyć średnich ocen przez pobieranie wszystkich opinii,
- mieć limit wyników,
- być odporny na pustą bazę.

## Checklist

- [ ] Dane nie są pobierane wielokrotnie bez potrzeby.
- [ ] Repository ukrywa źródło danych.
- [ ] ViewModel wystawia jeden spójny `UiState`.
- [ ] Cache jest używany świadomie.
- [ ] Listy mają limity i paginację.
- [ ] Mapa używa bounds/promienia.
- [ ] Ranking nie liczy się dynamicznie z pełnej bazy opinii.
=======
  -> Composable event
  -> ViewModel action
  -> Validation
  -> Use Case / Repository
  -> Firebase / Room
  -> Result
  -> UI State / UI Event
```

## UI

UI powinno:

- emitować akcje użytkownika do ViewModelu,
- obserwować UI state,
- renderować stany loading, success, empty, error,
- nie wykonywać bezpośrednich operacji na Firebase albo Room.

## ViewModel

ViewModel powinien:

- przyjmować akcje z UI,
- uruchamiać use case albo repozytorium,
- mapować wyniki na UI state,
- wystawiać StateFlow,
- wystawiać eventy jednorazowe, jeśli są potrzebne.

## Repository

Repository powinno:

- ukrywać szczegóły źródła danych,
- łączyć dane z Firebase i Room,
- mapować DTO/Entity na modele domenowe,
- zwracać wynik w kontrolowanej formie.

## Mappery

Mappery odpowiadają za konwersję:

```text
Firestore DTO -> Domain Model
Room Entity -> Domain Model
Domain Model -> DTO / Entity
```

Mapper nie powinien zawierać logiki UI.

## Obsługa błędów

Błąd techniczny powinien przejść przez mapowanie:

```text
Firebase Exception
  -> Error Mapper
  -> Domain/Error Result
  -> UiText / UI State
  -> komunikat dla użytkownika
```

## Offline-first

Dla ekranów listowych preferowany przepływ:

```text
Room cache -> UI
Firebase snapshot -> Room update -> UI refresh
```

Dzięki temu UI może działać na ostatnich znanych danych nawet przy słabym połączeniu.

## Checklist

- [ ] UI nie zna Firebase ani Room.
- [ ] ViewModel nie zwraca DTO.
- [ ] Repository mapuje dane do Domain Model.
- [ ] Błędy są mapowane przed pokazaniem użytkownikowi.
- [ ] Flow danych jest jednokierunkowy i przewidywalny.
>>>>>>> c171379 (docs: add data flow documentation)
