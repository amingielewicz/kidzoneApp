# Error Handling

Ostatnia aktualizacja: 2026-07-13

## Cel

Jednolita strategia obsługi błędów w kidZone. Użytkownik nie widzi wyjątków technicznych, a diagnostyka nie ujawnia danych prywatnych.

## Przepływ

```text
Technical error
  → mapper
  → domain result
  → UiText / UiState
  → user-facing message
```

## Kategorie błędów

- walidacja,
- brak internetu,
- timeout,
- uwierzytelnianie,
- Firestore,
- Storage i upload,
- uprawnienia,
- lokalizacja i usługi systemowe,
- konflikt danych,
- rate limit,
- błąd częściowy,
- błąd nieznany.

## Walidacja

Walidacja powinna być wykonywana możliwie wcześnie i przypisana do konkretnego pola lub akcji.

Przykłady:

- puste pole,
- błędny e-mail,
- niepoprawne hasło,
- zbyt długa opinia,
- brak wymaganych danych miejsca,
- przekroczony limit zdjęć.

Brak zgody na opcjonalną lokalizację lub kamerę nie powinien być traktowany jak błąd formularza blokujący całą funkcję.

## Mapowanie wyjątków

Wyjątki Firebase, Room i Androida są mapowane centralnie. UI nie otrzymuje:

- nazw klas wyjątków,
- stack trace,
- surowych kodów błędów,
- payloadów backendu,
- szczegółów reguł bezpieczeństwa.

## Typy prezentacji

UI rozróżnia:

- błąd blokujący ekran,
- błąd pojedynczej akcji,
- błąd pola formularza,
- snackbar lub dialog,
- offline state,
- permission state,
- błąd częściowy z możliwością retry.

## Retry

Retry powinno być:

- jawne dla użytkownika, jeśli operacja wymaga jego decyzji,
- automatyczne tylko dla bezpiecznych operacji,
- ograniczone liczbą prób i backoffem,
- idempotentne dla zapisów,
- odporne na podwójne kliknięcie.

## Uprawnienia

Obsługa uprawnień powinna rozróżniać:

- pierwszą prośbę,
- zwykłą odmowę,
- trwałą odmowę,
- wyłączoną usługę systemową,
- powrót z ustawień.

Trwała odmowa prowadzi do ustawień aplikacji. Akcja nie może pozostać martwa.

## UiText

Komunikaty używają zasobów tekstowych i są lokalizowane. Tekst techniczny z backendu nie trafia bezpośrednio do UI bez walidacji i mapowania.

## Logowanie błędów

Nie logujemy:

- haseł i tokenów,
- pełnych e-maili,
- dokładnej lokalizacji,
- treści opinii i formularzy,
- URI zdjęć,
- pełnych payloadów użytkownika.

Do logów i Crashlytics trafia bezpieczny kontekst: funkcja, operacja, wersja, typ błędu i status retry.

## Checklista

- [ ] wyjątek nie trafia bezpośrednio do UI,
- [ ] użytkownik dostaje zrozumiały komunikat,
- [ ] komunikat jest lokalizowany,
- [ ] błąd ma właściwy typ prezentacji,
- [ ] logi nie zawierają PII,
- [ ] retry jest ograniczone i idempotentne,
- [ ] błąd częściowy nie daje fałszywego sukcesu,
- [ ] odmowa uprawnienia nie tworzy martwej akcji.
