# Data Flow

## Cel

Dokument opisuje przepływ danych w KidZone: od UI, przez ViewModel i warstwę domenową, aż do Room, Firestore, Storage oraz Google Maps.

## Standardowy przepływ odczytu

```text
Composable
  -> ViewModel
  -> UseCase
  -> Repository
  -> Local cache / Remote source
  -> UiState
  -> Composable
```

## Standardowy przepływ zapisu

```text
User action
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
