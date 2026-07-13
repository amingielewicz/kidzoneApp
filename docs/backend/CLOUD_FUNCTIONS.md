# Cloud Functions

Ostatnia aktualizacja: 2026-07-13

## Cel

Dokument opisuje odpowiedzialności Cloud Functions w kidZone, typy triggerów oraz zasady bezpieczeństwa, idempotencji, kosztów i obsługi błędów.

## Kategorie funkcji

```text
Auth lifecycle
Moderation
Admin actions
Notifications
Ranking
Maintenance
Account deletion
```

## Auth lifecycle

### User created

Odpowiedzialność:

- utworzenie wymaganych danych profilu,
- ustawienie wartości domyślnych,
- bezpieczne zainicjalizowanie danych prywatnych,
- opcjonalne powiadomienie lub wiadomość powitalna.

Funkcja musi być odporna na ponowne wykonanie i nie może nadpisywać danych wprowadzonych przez użytkownika.

### User deleted

Odpowiedzialność:

- cleanup danych prywatnych,
- usunięcie lub anonimizacja profilu publicznego,
- cleanup plików i tokenów FCM,
- obsługa częściowego błędu i retry,
- zgodność z polityką account deletion.

Nie wysyłamy wiadomości po usunięciu konta, jeśli wymagałoby to zachowania e-maila dłużej niż jest to potrzebne.

## Moderation

Eventy zgłoszeń miejsca, opinii i zdjęcia powinny:

- walidować typ celu i identyfikator,
- blokować oczywiste duplikaty,
- zapisywać bezpieczny rekord audytowy,
- tworzyć zadanie dla panelu administracyjnego,
- nie kopiować zbędnych danych osobowych.

## Admin actions

Operacje takie jak usunięcie miejsca, opinii, zdjęcia lub zmiana danych użytkownika wymagają:

- uwierzytelnienia,
- weryfikacji roli administratora,
- walidacji payloadu,
- jawnego powodu operacji,
- audytu bez PII,
- idempotencji,
- kontrolowanego wyniku częściowego,
- aktualizacji agregatów i zależnych danych.

Klient nie może przekazywać roli ani pól administracyjnych jako zaufanego źródła.

## Notifications

Funkcje push:

- sprawdzają preferencje użytkownika,
- nie wysyłają do autora zdarzenia bez potrzeby,
- deduplikują powiadomienia,
- walidują target i deep link,
- sprzątają nieważne tokeny,
- nie umieszczają danych wrażliwych w payloadzie.

## Ranking i scheduled functions

- operacje mają limity i paginację,
- nie skanują całej bazy bez kontroli,
- zapisują checkpoint lub datę ostatniego wykonania,
- są odporne na ponowne uruchomienie,
- mają limit liczby powiadomień,
- błędy pojedynczego rekordu nie zatrzymują całej partii bez raportu.

## Walidacja wejścia

Każda funkcja callable lub HTTP powinna walidować:

- auth context,
- rolę,
- typy i długości pól,
- dozwolone wartości enum,
- identyfikatory zasobów,
- limity liczby elementów,
- ownership lub uprawnienie administracyjne.

## Bezpieczeństwo

- brak sekretów i tokenów w logach,
- brak pełnych e-maili, treści formularzy i dokładnej lokalizacji,
- App Check włączony tam, gdzie jest obsługiwany i uzasadniony,
- zasada najmniejszych uprawnień dla service account,
- brak zaufania do danych przesłanych przez klienta,
- dane administracyjne aktualizowane wyłącznie po stronie zaufanej.

## Idempotencja i retry

Funkcja powinna zakładać, że event może zostać dostarczony ponownie.

Stosujemy:

- identyfikator zdarzenia lub klucz deduplikacji,
- transakcje albo warunkowe aktualizacje,
- status operacji,
- bezpieczny retry z backoffem,
- dead-letter lub follow-up dla trwałych błędów.

Nie oznaczamy operacji jako zakończonej, jeśli cleanup albo zapis zależny nie został wykonany.

## Koszty i wydajność

- zapytania mają limity,
- batch ma kontrolowany rozmiar,
- operacje masowe są dzielone,
- unikamy N+1 reads,
- scheduled functions mają checkpointy,
- anomalie czasu wykonania i kosztów są monitorowane,
- funkcje nie uruchamiają nieograniczonej kaskady triggerów.

## Logowanie i monitoring

Bezpieczny log może zawierać:

- nazwę funkcji,
- typ operacji,
- status,
- wersję wdrożenia,
- liczbę przetworzonych rekordów,
- pseudonimizowany identyfikator korelacyjny.

Nie logujemy payloadów użytkownika ani pełnych tokenów.

## Testy

Wymagane są:

- build i lint,
- testy jednostkowe walidacji i auth,
- testy idempotencji,
- scenariusze błędu częściowego,
- testy nieważnego payloadu,
- manualny smoke na projekcie testowym dla funkcji krytycznych.

## Checklist PR

- [ ] odpowiedzialność funkcji jest jednoznaczna,
- [ ] auth, rola i App Check są zweryfikowane,
- [ ] payload ma walidację i limity,
- [ ] funkcja jest idempotentna,
- [ ] retry nie tworzy duplikatów,
- [ ] logi nie zawierają PII,
- [ ] wpływ na koszty jest oceniony,
- [ ] testy pokrywają sukces i błędy częściowe,
- [ ] wpływ na Data Safety i account deletion jest sprawdzony.

## Checklist release

- [ ] `npm run lint` przechodzi,
- [ ] `npm run build` przechodzi,
- [ ] testy przechodzą,
- [ ] deploy wykonano do właściwego projektu,
- [ ] konfiguracja i sekrety są właściwe,
- [ ] logi po deployu są czyste,
- [ ] funkcje krytyczne mają smoke PASS,
- [ ] monitoring i alerty są aktywne.
