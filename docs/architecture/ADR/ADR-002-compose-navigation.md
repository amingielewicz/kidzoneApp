# ADR-002: Use Navigation Compose

Ostatnia aktualizacja: 2026-07-14

## Status

Accepted

## Context

kidZone używa Jetpack Compose. Nawigacja musi wspierać spójny back stack, deep linki, stan po logout/delete account oraz bezpieczne argumenty ekranów.

## Decision

Głównym mechanizmem nawigacji jest Navigation Compose.

## Rules

- `NavController` pozostaje w warstwie UI,
- ViewModel emituje event lub wynik zamiast wykonywać nawigację,
- route przekazuje małe argumenty, głównie identyfikatory,
- pełne obiekty są ponownie pobierane po ID,
- argumenty są walidowane,
- nieważny lub brakujący zasób ma kontrolowany error state,
- deep link nie omija auth, ownership ani moderacji,
- logout, ban sign-out i account deletion czyszczą prywatny back stack,
- powiadomienie nie tworzy niepotrzebnych duplikatów ekranów.

## Consequences

### Positive

- zgodność ze stosem Compose,
- jeden graph dla ekranów i deep linków,
- przewidywalne testy route i back stack,
- oddzielenie stanu UI od mechanizmu nawigacji.

### Trade-offs

- złożone flow wymagają świadomego `popUpTo`, `inclusive` i `launchSingleTop`,
- błędne route mogą prowadzić do zduplikowanych ekranów,
- argumenty tekstowe wymagają kodowania i walidacji,
- proces restore może wymagać ponownego pobrania danych.

## Rejected alternatives

### Przekazywanie całych modeli przez route

Odrzucone z powodu rozmiaru, serializacji, nieaktualnych danych i problemów z process recreation.

### Nawigacja bezpośrednio z ViewModel

Odrzucona z powodu zależności od frameworka i trudniejszych testów.

## Validation

Zmiany graphu powinny sprawdzać:

- Back i Up,
- cold start z deep linka,
- deep link po wylogowaniu,
- brak zasobu,
- logout i delete account,
- powiadomienie otwierające już aktywny ekran,
- process recreation.
