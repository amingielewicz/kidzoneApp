# Backend Events

Ostatnia aktualizacja: 2026-07-13

## Cel

Dokument opisuje zdarzenia backendowe kidZone oraz przepływy między Firebase Authentication, Firestore, Storage, Cloud Functions i FCM.

## Typy eventów

```text
Auth events
Firestore events
Storage-related events
Scheduled events
Manual admin actions
Account deletion events
```

## Wspólne zasady

Każdy event powinien mieć:

- jednoznaczne źródło,
- jasno opisany skutek,
- identyfikator lub mechanizm deduplikacji,
- kontrolę auth i ownership tam, gdzie jest wymagana,
- obsługę retry,
- bezpieczny log bez danych osobowych,
- test sukcesu i co najmniej jednego błędu.

Event nie może zakładać idealnej kolejności wykonania ani dokładnie jednokrotnego dostarczenia.

## Auth events

### User created

Źródło:

```text
Firebase Authentication
```

Skutki:

- utworzenie wymaganych danych profilu,
- ustawienie wartości domyślnych,
- inicjalizacja danych prywatnych,
- opcjonalna wiadomość powitalna.

Powtórne wykonanie nie może nadpisać danych użytkownika ani utworzyć duplikatów.

### User deleted

Źródło:

```text
Firebase Authentication
```

Skutki:

- cleanup danych prywatnych,
- usunięcie lub anonimizacja profilu publicznego,
- cleanup Storage,
- usunięcie tokenów FCM,
- oznaczenie wyniku cleanup i retry błędów częściowych.

## Firestore events

### Review created, updated lub deleted

Źródło:

```text
reviews/{reviewId}
```

Skutki:

- aktualizacja `ratingAverage`,
- aktualizacja `reviewsCount`,
- sprawdzenie odznak,
- opcjonalne powiadomienie właściciela miejsca.

Agregaty muszą być odporne na ponowne wykonanie, równoległe zapisy i usunięcie opinii.

### Report created

Źródło:

```text
reports/{reportId}
```

Dla celu `place`, `review` lub `photo`:

- walidacja typu i identyfikatora,
- deduplikacja zgłoszenia,
- zapis do kolejki moderacji,
- opcjonalne powiadomienie administratora,
- powiązanie z właściwym zasobem.

Event nie powinien kopiować zbędnych danych użytkownika do rekordu moderacyjnego.

### Change request created

Źródło:

```text
changeRequests/{changeRequestId}
```

Skutki:

- walidacja propozycji,
- zapis statusu oczekującego,
- powiadomienie administratora,
- ochrona przed samodzielną zmianą statusu przez klienta.

## Storage-related events

### Photo uploaded

Źródło:

```text
Firebase Storage
```

Skutki mogą obejmować:

- walidację metadanych i ścieżki,
- aktualizację dokumentu miejsca lub opinii,
- aktualizację `photosCount`,
- cleanup pliku bez poprawnego rekordu,
- opcjonalne powiadomienie.

Trigger nie może tworzyć pętli aktualizacji ani ufać samemu rozszerzeniu pliku.

### Photo deleted

Skutki:

- usunięcie odnośnika z Firestore,
- aktualizacja licznika,
- bezpieczna obsługa brakującego dokumentu,
- zamknięcie odpowiedniego zgłoszenia, jeśli dotyczy.

## Scheduled events

### Ranking check

Źródło:

```text
Cloud Scheduler
```

Zasady:

- paginacja i limity,
- checkpoint ostatniego wykonania,
- limit powiadomień,
- brak pełnego skanu bez kontroli,
- błąd pojedynczego rekordu nie ukrywa wyniku całej partii.

### Maintenance

Scheduled cleanup może obejmować:

- nieważne tokeny FCM,
- osierocone pliki,
- wygasłe rekordy tymczasowe,
- nieudane operacje wymagające follow-up.

Retencja musi odpowiadać polityce prywatności i account deletion.

## Manual admin actions

Operacje usuwania miejsca, opinii, zdjęcia lub zmiany danych użytkownika powinny:

- weryfikować auth i rolę,
- walidować target,
- zapisywać powód i wynik,
- być idempotentne,
- aktualizować zależne dane,
- zamykać zgłoszenie moderacyjne,
- nie logować danych osobowych.

## Account deletion workflow

Przepływ może obejmować wiele systemów:

```text
request accepted
  → reauthentication verified
  → private Firestore cleanup
  → public content delete/anonymize
  → Storage cleanup
  → FCM cleanup
  → Auth delete
  → local session/cache cleanup
```

Kolejność musi być świadomie zaprojektowana. Częściowy błąd nie może zostać oznaczony jako pełny sukces.

## Idempotencja

Stosujemy:

- event ID lub klucz deduplikacji,
- statusy `pending`, `processing`, `completed`, `failed`,
- transakcje lub warunkowe aktualizacje,
- retry z backoffem,
- mechanizm dead-letter lub follow-up dla błędów trwałych.

## Monitoring

Monitorujemy:

- liczbę eventów,
- czas wykonania,
- retry i błędy trwałe,
- duplikaty,
- koszt odczytów i zapisów,
- liczbę rekordów w partii,
- błędy cleanup account deletion.

## Checklista

- [ ] event ma określone źródło i skutek,
- [ ] event jest odporny na ponowne wykonanie,
- [ ] retry nie tworzy duplikatów,
- [ ] auth i ownership są sprawdzone,
- [ ] logi nie zawierają PII,
- [ ] koszty i limity są kontrolowane,
- [ ] błąd częściowy jest widoczny,
- [ ] istnieje scenariusz testowy,
- [ ] retencja i cleanup są zgodne z dokumentami prawnymi.
