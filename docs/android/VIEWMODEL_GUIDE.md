# ViewModel Guide

## Cel

Dokument opisuje standardy projektowania ViewModeli w aplikacji Android KidZone.

## Zasady główne

- ViewModel zarządza stanem ekranu.
- ViewModel nie zna `NavController`.
- ViewModel nie powinien importować `android.content.Context`.
- Logika biznesowa powinna trafiać do use case albo repozytorium.
- UI obserwuje stan, ale nie przechowuje logiki biznesowej.

## UI State

Każdy większy ekran powinien mieć jawny UI state.

Przykład struktury:

```text
isLoading
isRefreshing
data
errorMessage
emptyState
```

## Eventy jednorazowe

Do zdarzeń jednorazowych należą:

- snackbar,
- toast,
- nawigacja po sukcesie,
- dialog błędu,
- komunikat po zapisie.

Nie powinny być trzymane jako zwykły trwały state, który może odtworzyć się po rotacji.

## StateFlow

Preferowane podejście:

- `StateFlow` dla stanu ekranu,
- `SharedFlow` albo kanał eventów dla zdarzeń jednorazowych,
- brak bezpośredniego mutowania stanu w Composable.

## Hilt

ViewModel powinien otrzymywać zależności przez konstruktor:

```text
repository
useCase
savedStateHandle
logger
analytics
```

## Obsługa błędów

ViewModel powinien:

- mapować wyjątki na komunikaty UI,
- nie pokazywać użytkownikowi technicznych wyjątków,
- logować błędy diagnostyczne bez danych wrażliwych.

## Testowalność

ViewModel jest dobrze zaprojektowany, jeśli można przetestować:

- loading state,
- success state,
- empty state,
- error state,
- reakcję na akcję użytkownika,
- zdarzenia jednorazowe.

## Checklist

- [ ] ViewModel nie zna NavController.
- [ ] ViewModel nie importuje Context.
- [ ] Stan ekranu jest jawny.
- [ ] Błędy są mapowane na komunikaty UI.
- [ ] Eventy jednorazowe nie są trwałym state.
- [ ] Logika biznesowa nie siedzi w Composable.
- [ ] ViewModel można przetestować jednostkowo.
