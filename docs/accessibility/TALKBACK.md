# TalkBack Guide

Ostatnia aktualizacja: 2026-07-14

## Cel

Standard projektowania i testowania kidZone z Android TalkBack.

## Zasady semantyki

- każdy element interaktywny ma czytelną nazwę, rolę i stan,
- ikony dekoracyjne nie są odczytywane,
- złożone karty są grupowane logicznie,
- etykieta opisuje skutek działania, nie sam wygląd ikony,
- zaznaczenie, rozwinięcie, loading, błąd i disabled state są dostępne,
- element klikalny nie ma zduplikowanej akcji w dzieciach bez wyraźnej potrzeby.

## Fokus

- kolejność fokusu odpowiada kolejności wizualnej i logicznej,
- fokus nie przeskakuje po recomposition,
- loading i odświeżenie nie kradną fokusu wielokrotnie,
- dialog i bottom sheet przejmują fokus,
- po zamknięciu fokus wraca do elementu, który otworzył warstwę,
- po nawigacji fokus trafia do nagłówka lub pierwszego logicznego elementu.

## Komunikaty dynamiczne

Należy ogłaszać:

- błąd formularza,
- zmianę statusu lokalizacji,
- wynik zapisu,
- ważny snackbar,
- offline state,
- brak wyników,
- zakończenie uploadu lub częściowy błąd.

Nie ogłaszamy wielokrotnie tego samego stanu przy każdej recomposition.

## Formularze

- pola odczytują etykietę, wartość i stan błędu,
- pole hasła ma akcję „Pokaż hasło” albo „Ukryj hasło”,
- wymagane pola są oznaczone tekstowo,
- walidacja prowadzi do pierwszego błędnego pola lub jasno ogłasza problem,
- klawiatura i fokus poruszają się w logicznej kolejności,
- zapis ma dostępny stan loading i disabled,
- zdjęcie oraz przycisk jego usunięcia mają jednoznaczne etykiety.

## Mapa

Mapa nie jest jedyną drogą wykonania zadania.

- „Moja lokalizacja” ma czytelną nazwę,
- brak zgody i wyłączony GPS mają tekstowy fallback,
- lista miejsc jest dostępna z mapy,
- karta fallbacku odczytuje nazwę, kategorię, dystans i ocenę,
- marker nie jest jedynym sposobem przejścia do szczegółów,
- bottom sheet ma poprawną kolejność fokusu.

## Uprawnienia

- uzasadnienie jest odczytywane przed dialogiem systemowym,
- odmowa ma dostępny dalszy krok,
- trwała odmowa prowadzi do ustawień aplikacji,
- po powrocie stan jest ponownie sprawdzany i ogłaszany,
- brak lokalizacji, kamery lub powiadomień nie blokuje całego flow.

## Scenariusze testowe Android

- Splash i onboarding,
- Logowanie, Rejestracja i reset hasła,
- Start,
- Lista i wyszukiwanie,
- Mapa i fallback listy,
- Szczegóły miejsca,
- Dodawanie miejsca,
- Dodawanie opinii i zdjęć,
- Ranking,
- Profil, logout i account deletion,
- dialogi, bottom sheety i snackbary,
- offline, błędy i trwałe odmowy uprawnień,
- deep link i powiadomienie.

## Sposób testowania

1. Włącz TalkBack.
2. Korzystaj głównie z nawigacji gestami „następny/poprzedni”.
3. Nie polegaj wyłącznie na eksploracji dotykiem.
4. Sprawdź ekran od początku do końca.
5. Powtórz test po rotacji, powrocie z ustawień i zmianie stanu.
6. Sprawdź fizyczne urządzenie, jeśli flow jest krytyczne.

## Typowe problemy

- odczytywanie „przycisk bez etykiety”,
- kilka pustych elementów fokusu,
- podwójne odczytanie karty i jej dzieci,
- fokus uciekający po odświeżeniu,
- snackbar nieogłaszany albo ogłaszany wielokrotnie,
- dialog bez poprawnego focus trap,
- ikona „X” bez informacji, co usuwa,
- status przekazywany wyłącznie kolorem.

## Release gate

- [ ] krytyczne flow można ukończyć bez eksploracji dotykiem,
- [ ] wszystkie akcje mają czytelne etykiety,
- [ ] fokus jest logiczny i stabilny,
- [ ] dynamiczne komunikaty są ogłaszane raz,
- [ ] formularze odczytują błędy,
- [ ] mapa ma dostępny fallback,
- [ ] uprawnienia nie tworzą ślepego zaułka,
- [ ] dialogi i bottom sheety poprawnie zarządzają fokusem,
- [ ] brak pustych i zduplikowanych elementów semantycznych.
