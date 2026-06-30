# State Management

## Cel

Dokument opisuje zasady zarządzania stanem w aplikacji KidZone.

## Główna zasada

Stan ekranu powinien mieć jedno źródło prawdy.

```text
Repository / Use Case
        ↓
ViewModel
        ↓
StateFlow<UiState>
        ↓
Compose UI
```

## UI State

Każdy większy ekran powinien posiadać jawny `UiState`.

Typowe pola:

```text
isLoading
isRefreshing
isSaving
data
errorMessage
emptyState
selectedFilters
```

## StateFlow

`StateFlow` jest preferowany dla trwałego stanu ekranu.

Przykłady trwałego stanu:

- lista miejsc,
- wybrane filtry,
- aktualny użytkownik,
- loading state,
- error state,
- dane formularza.

## Eventy jednorazowe

Eventy jednorazowe nie powinny być trzymane jako trwały state.

Przykłady:

- snackbar,
- toast,
- nawigacja po sukcesie,
- jednorazowy dialog,
- komunikat po zapisie.

Do tego można używać `SharedFlow` albo dedykowanego event stream.

## Compose

Composable powinien:

- obserwować state,
- renderować UI,
- przekazywać akcje do ViewModelu,
- nie przechowywać logiki biznesowej.

Dopuszczalne lokalne state:

- stan rozwinięcia menu,
- lokalny tekst pola przed zatwierdzeniem,
- widoczność sheet/dialog,
- scroll state.

## ViewModel

ViewModel powinien:

- agregować dane z repozytoriów,
- przekształcać dane na UI state,
- obsługiwać akcje użytkownika,
- mapować błędy na komunikaty UI.

## Antywzorce

- kilka niezależnych źródeł prawdy dla tego samego ekranu,
- trzymanie snackbarów jako trwałego state,
- mutowanie listy bez copy/update,
- logika biznesowa w Composable,
- bezpośrednie requesty z UI.

## Checklist

- [ ] Ekran ma jedno źródło prawdy.
- [ ] UiState jest jawny.
- [ ] Eventy jednorazowe nie są trwałym state.
- [ ] Compose tylko renderuje UI.
- [ ] ViewModel mapuje dane na stan ekranu.
