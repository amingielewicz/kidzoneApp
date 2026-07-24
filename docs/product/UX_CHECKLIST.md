# UX Checklist

Ostatnia aktualizacja: 2026-07-13

## Cel

Lista kontrolna UX dla kidZone. Stosować przy zmianach funkcjonalnych, większych zmianach ekranów oraz przed release.

## Pierwsze użycie

- [ ] użytkownik rozumie cel aplikacji,
- [ ] główne funkcje są jasne: Start, Mapa, Lista, Ranking, Profil i Dodaj miejsce,
- [ ] onboarding jest krótki i możliwy do pominięcia,
- [ ] zgody nie są proszone wszystkie naraz,
- [ ] po pierwszym uruchomieniu istnieje oczywisty następny krok.

## Nawigacja

- [ ] dolna nawigacja jest czytelna,
- [ ] aktualny ekran jest wyróżniony także inaczej niż kolorem,
- [ ] Back działa przewidywalnie,
- [ ] po logout i delete account nie można wrócić do prywatnych ekranów,
- [ ] deep link otwiera właściwy zasób albo kontrolowany błąd,
- [ ] przełączanie zakładek nie resetuje niepotrzebnie stanu.

## Wyszukiwanie, filtry i lista

- [ ] wyszukiwarka jest łatwa do znalezienia,
- [ ] filtry i sortowanie są zrozumiałe,
- [ ] sortowanie „Od najbliższych” poprawnie obsługuje brak lokalizacji,
- [ ] brak wyników ma empty state i sensowne CTA,
- [ ] zmiana filtrów daje szybki feedback,
- [ ] brak internetu nie powoduje utraty działającego cache.

## Mapa

- [ ] znaczenie markerów jest zrozumiałe,
- [ ] kliknięcie markera otwiera właściwy bottom sheet,
- [ ] „Moja lokalizacja” ma działające flow zgody,
- [ ] trwała odmowa prowadzi do ustawień,
- [ ] wyłączony GPS ma osobny komunikat,
- [ ] lista pozostaje alternatywą dla mapy,
- [ ] mapa nie pobiera danych bez końca podczas ruchu.

## Dodawanie treści

- [ ] formularz jest logicznie podzielony,
- [ ] błędy walidacji są przypisane do pól,
- [ ] dane nie znikają po błędzie,
- [ ] użytkownik widzi stan zapisu,
- [ ] double submit jest zablokowany,
- [ ] Photo Picker działa bez szerokiej zgody do galerii,
- [ ] brak kamery nie blokuje wyboru istniejącego zdjęcia,
- [ ] sukces jest pokazany dopiero po pełnym zakończeniu operacji.

## Ranking i profil

- [ ] użytkownik rozumie podstawę rankingu,
- [ ] ocena i liczba opinii są czytelne,
- [ ] pusty ranking ma właściwy stan,
- [ ] profil jasno oddziela treści publiczne od ustawień konta,
- [ ] edycja danych, logout i account deletion są łatwe do znalezienia,
- [ ] konto usunięte lub zablokowane nie pozostawia aktywnych prywatnych widoków.

## Uprawnienia

- [ ] zgoda jest proszona dopiero przy użyciu funkcji,
- [ ] użytkownik rozumie cel zgody,
- [ ] odmowa nie blokuje całej aplikacji,
- [ ] ponowne kliknięcie działa,
- [ ] trwała odmowa nie tworzy martwego przycisku,
- [ ] powrót z ustawień odświeża stan,
- [ ] odmowa powiadomień nie wpływa na podstawowe funkcje.

## Offline i błędy

- [ ] cached data jest nadal użyteczne,
- [ ] zapis offline nie udaje sukcesu,
- [ ] komunikat rozróżnia brak internetu, błąd uprawnień i błąd serwera,
- [ ] retry jest dostępne tam, gdzie ma sens,
- [ ] błąd częściowy nie jest pokazany jako pełny sukces,
- [ ] użytkownik nie widzi surowych wyjątków.

## Dostępność UX

- [ ] flow można ukończyć z TalkBack,
- [ ] duża czcionka nie ukrywa CTA,
- [ ] kolejność fokusu jest logiczna,
- [ ] komunikaty są ogłaszane,
- [ ] wszystkie funkcje mapy mają alternatywę,
- [ ] kolor nie jest jedynym nośnikiem informacji.

## Manualny test UX

- [ ] użytkownik znajduje miejsce bez pomocy,
- [ ] filtruje i sortuje listę,
- [ ] korzysta z mapy bez udzielania lokalizacji,
- [ ] dodaje miejsce i opinię,
- [ ] wybiera zdjęcie przez Photo Picker,
- [ ] znajduje ustawienia konta i usunięcie konta,
- [ ] rozumie, co zrobić przy braku internetu, GPS lub trwałej odmowie zgody.
