# ADR-004: Use StateFlow for Screen State

Ostatnia aktualizacja: 2026-07-14

## Status

Accepted

## Context

Ekrany kidZone obsługują loading, content, empty, error, refresh, offline i permission states. Compose działa najlepiej, gdy renderuje niezmienny stan pochodzący z jednego źródła prawdy.

## Decision

ViewModele udostępniają trwały stan ekranu przez `StateFlow<UiState>`.

Jednorazowe efekty, takie jak nawigacja, snackbar lub otwarcie ustawień, korzystają z osobnego mechanizmu efektów, na przykład `SharedFlow` albo kanału konsumowanego jednokrotnie.

## Rules

- `UiState` jest niezmienny,
- ekran renderuje się wyłącznie na podstawie stanu,
- akcje użytkownika trafiają do ViewModel,
- aktualizacja stanu jest atomowa,
- event jednorazowy nie jest przechowywany jako trwały boolean,
- stan permission flow odzwierciedla rzeczywisty stan systemu,
- offline, error i partial success są rozróżnione,
- stan nie zawiera `Context`, `NavController` ani typów Firebase,
- kolekcja flow w Compose respektuje lifecycle.

## Consequences

### Positive

- przewidywalne renderowanie,
- prostsze testy ViewModeli,
- jedno źródło prawdy,
- łatwiejsza obsługa process recreation,
- czytelne oddzielenie stanu i efektów.

### Trade-offs

- więcej jawnych modeli `UiState`,
- ryzyko zbyt dużych klas stanu,
- błędne `combine` może powodować nadmiarowe emisje,
- efekty jednorazowe wymagają świadomego wzorca,
- nieuważne porównania mogą zwiększyć recomposition.

## Rejected alternatives

### Wiele niezależnych `LiveData` lub flow dla jednego ekranu

Odrzucone z powodu trudności w utrzymaniu spójnego snapshotu stanu.

### Booleany typu `showSnackbar`

Odrzucone, ponieważ mogą zostać ponownie odtworzone po rotacji lub recomposition.

## Validation

Testy powinny obejmować:

- loading → content,
- loading → error,
- refresh z istniejącą treścią,
- offline z cache i bez cache,
- jednorazowy efekt bez powtórzenia,
- równoległe akcje,
- powrót z ustawień systemowych.
