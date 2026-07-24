# Firestore Security Rules

Ostatnia aktualizacja: 2026-07-13

## Cel

Zasady projektowania, testowania i wdrażania Firestore Security Rules w kidZone.

## Zasady główne

- `deny by default`,
- dostęp tylko dla jawnie opisanych przypadków,
- brak zaufania do walidacji klienta,
- ownership sprawdzany przez `request.auth.uid`,
- role administracyjne pochodzą z zaufanego źródła,
- typy, zakresy i dozwolone pola są walidowane,
- klient nie może zmieniać pól administracyjnych ani agregatów,
- dane publiczne i prywatne są rozdzielone.

## Użytkownicy

Publiczny profil może być czytelny zgodnie z produktem, ale nie zawiera e-maila, tokenów FCM ani danych prywatnych.

Dane prywatne w `users/{uid}/private/*` są dostępne tylko właścicielowi lub upoważnionemu administratorowi.

Użytkownik nie może samodzielnie ustawić między innymi:

- roli admina,
- `isBanned`,
- pól rankingowych,
- liczników moderacyjnych,
- danych innego użytkownika.

## Miejsca

- publiczny odczyt tylko dla dozwolonych statusów,
- tworzenie przez zalogowanego użytkownika,
- `createdBy` musi odpowiadać UID,
- autor edytuje wyłącznie dozwolone pola,
- status moderacji i pola agregowane są chronione,
- usuwanie i moderacja zgodnie z rolą i polityką produktu.

## Opinie

- tworzenie przez zalogowanego użytkownika,
- `userId` odpowiada UID,
- `rating` ma zakres 1–5,
- autor zarządza własną opinią,
- pola moderacyjne i agregaty nie są edytowalne przez klienta,
- reguły ograniczają niepożądane dodatkowe pola.

## Zgłoszenia i change requests

- użytkownik może utworzyć zgłoszenie lub propozycję zmiany,
- autor nie może ustawić statusu rozpatrzenia, `resolvedBy`, `reviewedBy` ani pól administracyjnych,
- odczyt cudzych zgłoszeń jest zabroniony,
- obsługa kolejki moderacji wymaga roli administratora.

## Powiadomienia i dane prywatne

- użytkownik czyta wyłącznie własne powiadomienia,
- tokeny FCM nie znajdują się w publicznych dokumentach,
- klient nie może tworzyć dowolnego powiadomienia dla innego użytkownika,
- zapis systemowy odbywa się po stronie zaufanej.

## Walidacja

Rules powinny sprawdzać:

- wymagane pola,
- typy,
- maksymalne długości tekstu,
- zakresy liczb,
- dozwolone enumy,
- ownership,
- brak dodatkowych niedozwolonych pól,
- niezmienność pól systemowych,
- serwerowe znaczniki czasu tam, gdzie to możliwe.

## Testy

Każda zmiana Rules wymaga testów emulatorowych dla:

- dozwolonego odczytu i zapisu,
- niezalogowanego użytkownika,
- obcego właściciela,
- próby eskalacji roli,
- zmiany chronionego pola,
- błędnego typu i wartości granicznej,
- nieznanego dodatkowego pola,
- administratora,
- danych prywatnych.

## Wdrożenie

1. Uruchom testy emulatora.
2. Wykonaj review diffu Rules.
3. Wdróż do projektu testowego.
4. Wykonaj smoke aplikacji.
5. Wdróż do produkcji przed buildem zależnym od nowej reguły.
6. Monitoruj błędy `permission-denied`.

## Checklista

- [ ] deny by default,
- [ ] ownership sprawdzony,
- [ ] role i pola administracyjne chronione,
- [ ] publiczne dokumenty nie zawierają PII,
- [ ] prywatne ścieżki mają ograniczony odczyt,
- [ ] typy, zakresy i dodatkowe pola są walidowane,
- [ ] testy emulatora przechodzą,
- [ ] smoke po deployu ma PASS,
- [ ] rollback lub poprzednia wersja Rules jest dostępna.
