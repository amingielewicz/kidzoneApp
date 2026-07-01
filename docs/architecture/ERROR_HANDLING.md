# Error Handling

## Cel

Dokument opisuje jednolitą strategię obsługi błędów w aplikacji KidZone.

## Główna zasada

Użytkownik nie powinien widzieć technicznych wyjątków.

```text
Technical Error
  -> Mapper
  -> Domain Result
  -> UiText / UiState
  -> User Message
```

## Kategorie błędów

- walidacja formularza,
- brak połączenia z internetem,
- timeout,
- błąd Firebase Authentication,
- błąd Firestore,
- błąd Storage,
- brak uprawnień,
- konflikt danych,
- błąd nieznany.

## Walidacja

Błędy walidacji powinny być wykrywane możliwie wcześnie.

Przykłady:

- puste wymagane pole,
- niepoprawny e-mail,
- zbyt krótkie hasło,
- zbyt długa opinia,
- brak lokalizacji przy dodawaniu miejsca.

## Firebase errors

Błędy Firebase powinny być mapowane w jednej warstwie.

Nie pokazujemy użytkownikowi:

- nazw klas wyjątków,
- stack trace,
- technicznych kodów błędów bez tłumaczenia,
- pełnych szczegółów bezpieczeństwa.

## UiText

Komunikaty błędów w UI powinny używać zasobów tekstowych.

Dopuszczalne przypadki dynamicznego tekstu:

- komunikat diagnostyczny w debug build,
- tekst z backendu zaakceptowany jako user-facing,
- fallback dla nieznanego błędu.

## Logowanie błędów

Logi powinny pomagać w diagnostyce, ale nie mogą ujawniać danych wrażliwych.

Nie logujemy:

- haseł,
- tokenów,
- pełnych danych prywatnych,
- pełnych payloadów zawierających dane użytkownika.

## UI

UI powinno rozróżniać:

- błąd blokujący ekran,
- błąd akcji użytkownika,
- błąd do pokazania w snackbarze,
- błąd formularza przy konkretnym polu.

## Checklist

- [ ] Wyjątek techniczny nie trafia bezpośrednio do UI.
- [ ] Błąd ma user-friendly komunikat.
- [ ] Komunikat jest lokalizowany.
- [ ] Błąd jest logowany bez danych wrażliwych.
- [ ] UI pokazuje właściwy typ błędu.