# UI / UX Guidelines

Ostatnia aktualizacja: 2026-07-13

## Cel

Standardy UI i UX kidZone. Interfejs ma być spójny, lekki, dostępny i zrozumiały bez znajomości technicznych szczegółów aplikacji.

## Zasady UI

- Interfejs jest jasny, czytelny i rodzinny.
- Karty mają spójne odstępy, radius i hierarchię.
- Kategorie są prezentowane przez wspólny `CategoryBadge` lub spójną ikonę.
- Typografia ma stałą hierarchię.
- Główna akcja jest widoczna i jednoznaczna.
- FAB jasno komunikuje „Dodaj miejsce”.
- Nie duplikujemy wzorców UI między ekranami.

## Typografia i treść

- Tytuły są krótkie i konkretne.
- Opisy nie tworzą ścian tekstu.
- Komunikaty błędów nie zawierają technicznego żargonu.
- CTA opisuje skutek działania.
- Teksty działają przy dużej czcionce.
- Nie używamy samego koloru do przekazywania statusu.

## Karty miejsc

Karta może zawierać:

- nazwę,
- kategorię,
- ocenę i liczbę opinii,
- adres lub dystans,
- krótki opis,
- jedno główne CTA.

Karta na Starcie, Liście, Mapie i Rankingu powinna używać tych samych zasad prezentacji kategorii i ocen.

## Stany ekranu

Każdy główny ekran obsługuje odpowiednie stany:

- loading,
- success,
- empty,
- error,
- offline,
- permission required,
- service disabled,
- saving lub submitting.

Stan nie może pozostawiać użytkownika bez informacji lub akcji.

## Empty state

Dobry pusty stan zawiera:

- prosty nagłówek,
- krótkie wyjaśnienie,
- sensowną akcję, jeśli użytkownik może rozwiązać problem.

```text
Brak miejsc w okolicy
Nie znaleźliśmy jeszcze miejsc w tym obszarze.
Dodaj pierwsze miejsce
```

## Uprawnienia

- lokalizacja, kamera i powiadomienia są opcjonalne,
- przed systemowym dialogiem użytkownik rozumie, po co potrzebna jest zgoda,
- odmowa nie blokuje całej aplikacji,
- trwała odmowa prowadzi do ustawień aplikacji,
- przycisk nie może stać się martwy po kolejnej odmowie,
- po powrocie z ustawień ekran odświeża stan,
- Photo Picker nie prosi o szeroki dostęp do galerii.

## Formularze

- wymagane pola są jasno oznaczone,
- błąd jest pokazany przy właściwym polu,
- dane nie znikają po błędzie sieci,
- przycisk zapisu pokazuje stan działania,
- double submit jest blokowany,
- retry nie tworzy duplikatów,
- limit zdjęć i tekstu jest komunikowany przed błędem.

## Onboarding

Onboarding:

- wyjaśnia cel aplikacji,
- przedstawia Start, Mapę, Listę, Ranking i Profil,
- pokazuje, jak dodać miejsce,
- nie prosi przedwcześnie o wszystkie zgody,
- daje się pominąć,
- nie blokuje podstawowego poznania aplikacji.

## Dostępność

- ikony akcji mają opisy,
- touch targety są wygodne,
- kolejność fokusu jest logiczna,
- duża czcionka nie zasłania kluczowych akcji,
- lista jest alternatywą dla mapy,
- loading i błędy są komunikowane TalkBack,
- dialogi i bottom sheety poprawnie przejmują fokus.

## Offline i błędy

- dane z cache są oznaczone zachowaniem, nie technicznym komunikatem,
- brak internetu ma kontrolowany stan i retry,
- zapis offline nie może udawać sukcesu,
- użytkownik nie widzi surowych wyjątków,
- błąd częściowy nie kończy się fałszywym komunikatem sukcesu.

## Checklist

- [ ] główna akcja jest oczywista,
- [ ] użytkownik wie, gdzie się znajduje,
- [ ] ekran ma kompletne stany,
- [ ] teksty są krótkie i zrozumiałe,
- [ ] uprawnienia mają działający fallback,
- [ ] formularze nie tracą danych,
- [ ] UI jest dostępne z TalkBack i dużą czcionką,
- [ ] komponenty są spójne między ekranami,
- [ ] brak sieci i odmowy nie tworzą ślepych zaułków.
