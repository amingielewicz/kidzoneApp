# Cloud Functions

## Cel

Dokument opisuje odpowiedzialności Cloud Functions w KidZone, typy triggerów oraz zasady bezpieczeństwa i utrzymania.

## Kategorie funkcji

```text
Auth lifecycle
Moderation
Admin actions
Notifications
Ranking
Maintenance
```

## Auth lifecycle

### onUserCreated

Odpowiedzialność:

- inicjalizacja danych użytkownika,
- email powitalny,
- powiadomienie administratora, jeśli wymagane.

Ryzyka:

- błędne dane profilu,
- zbyt szerokie logowanie danych użytkownika.

### onUserDeleted

Odpowiedzialność:

- sprzątanie danych powiązanych z kontem,
- email pożegnalny,
- powiadomienie administratora, jeśli wymagane.

## Moderation

### onPlaceReport

Odpowiedzialność:

- obsługa zgłoszenia miejsca,
- powiadomienie administratora,
- przygotowanie danych do panelu admina.

### onReviewReport

Odpowiedzialność:

- obsługa zgłoszenia opinii,
- powiadomienie administratora,
- powiązanie zgłoszenia z opinią i miejscem.

### onPhotoReport

Odpowiedzialność:

- obsługa zgłoszenia zdjęcia,
- powiadomienie administratora,
- przygotowanie akcji administracyjnych.

## Admin actions

### adminDeletePlace

Odpowiedzialność:

- usunięcie lub oznaczenie miejsca jako usunięte,
- zapis powodu,
- powiadomienie użytkownika, jeśli wymagane.

Wymagania:

- auth check,
- admin role check,
- audit log albo wystarczający zapis operacji.

### adminDeleteReview

Odpowiedzialność:

- usunięcie opinii,
- aktualizacja pól ratingowych miejsca,
- powiadomienie autora, jeśli wymagane.

### adminDeletePhoto

Odpowiedzialność:

- usunięcie zdjęcia ze Storage,
- aktualizacja dokumentu miejsca,
- zamknięcie zgłoszenia, jeśli dotyczy.

### adminUpdateUserEmail

Odpowiedzialność:

- aktualizacja emaila w Auth,
- aktualizacja danych pomocniczych w Firestore, jeśli istnieją.

Wymagania:

- bardzo ścisła walidacja admina,
- brak logowania pełnych danych wrażliwych.

## Notifications

### onReviewCreatedPush

Odpowiedzialność:

- wysłanie push do właściciela miejsca po dodaniu opinii.

### onBadgeEarned

Odpowiedzialność:

- wysłanie push o zdobyciu odznaki.

### onPhotoAddedToPlace

Odpowiedzialność:

- wysłanie push o nowym zdjęciu miejsca.

### onUserBanned

Odpowiedzialność:

- powiadomienie użytkownika o blokadzie konta.

## Ranking

### dailyRankingCheck

Odpowiedzialność:

- sprawdzenie zmian w rankingu,
- wysłanie powiadomień o awansie,
- ograniczenie liczby operacji przez limity.

## Zasady bezpieczeństwa

- Każda funkcja HTTP admina musi sprawdzać auth.
- Każda funkcja HTTP admina musi sprawdzać rolę admina.
- Funkcje nie ufają danym z klienta.
- Funkcje nie logują tokenów, haseł, pełnych danych prywatnych ani sekretów.
- Błędy powinny być logowane w sposób diagnostyczny, ale bez danych wrażliwych.

## Zasady wydajności

- Funkcje powinny mieć limity zapytań.
- Operacje masowe powinny być dzielone na batch.
- Funkcje scheduled nie powinny przetwarzać całej bazy bez kontroli.
- Funkcje powinny być idempotentne tam, gdzie to możliwe.

## Checklist PR

- [ ] Funkcja ma jasno opisaną odpowiedzialność.
- [ ] Auth check jest obecny, jeśli wymagany.
- [ ] Admin role check jest obecny dla funkcji admina.
- [ ] Dane wejściowe są walidowane.
- [ ] Funkcja nie loguje danych wrażliwych.
- [ ] Funkcja ma test albo manualny scenariusz weryfikacji.
- [ ] Wpływ na koszty został sprawdzony.

## Checklist release

- [ ] Funkcje budują się poprawnie.
- [ ] ESLint przechodzi.
- [ ] TypeScript build przechodzi.
- [ ] Deploy wykonany na właściwy projekt Firebase.
- [ ] Logi funkcji po deployu nie pokazują błędów.
- [ ] Funkcje krytyczne sprawdzone manualnie.
