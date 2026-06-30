# Firestore Security Rules

## Cel

Dokument opisuje zasady projektowania i utrzymania Firestore Security Rules w projekcie KidZone.

## Główne zasady

- Domyślnie odmawiaj dostępu (`deny by default`).
- Dostęp przyznawaj wyłącznie dla jasno określonych przypadków.
- Waliduj typy i wymagane pola.
- Sprawdzaj ownership dokumentów.
- Nie ufaj walidacji po stronie aplikacji.

## Dostęp do kolekcji

### users
- użytkownik odczytuje i edytuje własne dane,
- administrator ma pełny dostęp.

### places
- odczyt publicznych miejsc,
- tworzenie tylko dla zalogowanych,
- edycja zgodnie z polityką produktu lub przez administratora.

### reviews
- tworzenie tylko przez zalogowanych,
- edycja i usuwanie przez autora lub administratora.

### reports
- zgłoszenie tworzy zalogowany użytkownik,
- odczyt i obsługa tylko dla administratorów.

### changeRequests
- tworzenie przez użytkowników,
- akceptacja lub odrzucenie przez administratora.

## Walidacja

- wymagane pola nie mogą być puste,
- ocena w zakresie 1–5,
- identyfikatory zgodne z właścicielem dokumentu,
- znaczniki czasu ustawiane przez serwer tam, gdzie to możliwe.

## Checklist

- [ ] Ownership sprawdzony.
- [ ] Typy pól walidowane.
- [ ] Brak publicznego dostępu do danych prywatnych.
- [ ] Rules pokryte testami emulatora.
- [ ] Zmiany Rules przechodzą review przed wdrożeniem.
