# ViewModel Guide

Ostatnia aktualizacja: 2026-07-13

## Cel

Standard projektowania ViewModeli w kidZone: jedno źródło prawdy, brak zależności od UI i Android `Context`, kontrolowane eventy oraz testowalna logika.

## Zasady główne

- ViewModel zarządza stanem ekranu.
- ViewModel nie zna `NavController`.
- ViewModel nie importuje `Activity`, `View` ani `Context`.
- Logika biznesowa trafia do use case'a lub repozytorium.
- Operacje platformowe są realizowane przez porty, eventy lub callbacki.
- UI obserwuje stan i wysyła akcje.

## UiState

Większy ekran powinien mieć jawny, niemutowalny `UiState` zawierający odpowiednie elementy:

- dane,
- loading i refreshing,
- saving lub submitting,
- empty state,
- błąd,
- filtry,
- offline state,
- stan zgody lub usługi systemowej.

Aktualizacje powinny być atomowe i wykonywane przez `copy`.

## Eventy jednorazowe

Eventami są między innymi:

- snackbar,
- nawigacja po sukcesie,
- otwarcie ustawień aplikacji,
- uruchomienie Photo Pickera lub kamery,
- jednorazowy dialog,
- komunikat po zapisie.

Nie przechowujemy ich jako trwałego boolean w `UiState`. Można użyć `SharedFlow`, kanału eventów albo callbacku z UI.

## StateFlow

- `StateFlow` dla trwałego stanu,
- `SharedFlow` lub inny event stream dla zdarzeń,
- lifecycle-aware collection w Compose,
- brak bezpośredniej mutacji stanu z UI.

## Zależności Hilt

ViewModel może otrzymywać przez konstruktor:

```text
use cases
repository interfaces
SavedStateHandle
error mapper
logger facade
analytics facade
platform ports
```

Logger i Analytics muszą przyjmować bezpieczne, ograniczone dane, a nie surowe modele użytkownika.

## Obsługa akcji

Preferowany model:

```text
UI intent
  → ViewModel action
  → validation
  → use case
  → result mapping
  → UiState update / one-off event
```

Akcje zapisu powinny być odporne na double submit i retry.

## Błędy

ViewModel:

- mapuje błędy domenowe na `UiText`,
- nie pokazuje surowych wyjątków,
- nie loguje PII,
- rozróżnia błąd ekranu, akcji, pola i offline,
- zachowuje dane formularza przy błędzie,
- nie daje fałszywego sukcesu przy częściowym niepowodzeniu.

## Uprawnienia

ViewModel nie wywołuje systemowego dialogu bezpośrednio. Może wystawić stan lub event wskazujący, że UI powinno:

- poprosić o zgodę,
- otworzyć ustawienia aplikacji,
- otworzyć ustawienia lokalizacji.

Po powrocie z ustawień rzeczywisty stan jest ponownie sprawdzany.

## Coroutine i lifecycle

- operacje są uruchamiane w `viewModelScope`,
- długie zadania mają timeout lub kontrolowane anulowanie,
- wyjątki nie są ignorowane,
- równoległe requesty nie nadpisują nowszego wyniku starym,
- obserwacje są zamykane zgodnie z lifecycle ViewModelu.

## Testowalność

Testy powinny pokrywać:

- loading, success, empty, error i offline,
- walidację,
- akcje użytkownika,
- eventy jednorazowe,
- double submit,
- retry,
- powrót z ustawień,
- race conditions i anulowanie.

## Checklista

- [ ] ViewModel nie zna `NavController`,
- [ ] ViewModel nie importuje `Context`,
- [ ] stan jest jawny i niemutowalny,
- [ ] eventy nie są trwałym state,
- [ ] błędy są mapowane,
- [ ] logika biznesowa jest poza Composable,
- [ ] zapisy są odporne na double submit,
- [ ] operacje platformowe są odseparowane,
- [ ] ViewModel ma testy jednostkowe.
