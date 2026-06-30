# Maps Handbook

## Cel

Dokument opisuje zasady użycia Google Maps w KidZone, z naciskiem na UX, wydajność i koszty.

## Rola mapy

Mapa służy do:

- odkrywania miejsc w pobliżu,
- szybkiej orientacji przestrzennej,
- wyboru najbliższego miejsca,
- przejścia do szczegółów miejsca,
- otwarcia nawigacji w Google Maps.

Mapa nie powinna być jedynym sposobem dostępu do danych. Każde miejsce widoczne na mapie powinno być dostępne również przez listę.

## Ładowanie danych

Zasady:

- Nie pobieramy całej kolekcji miejsc na ekran mapy.
- Pobieramy miejsca dla bounds albo promienia.
- Request wykonujemy dopiero po ustabilizowaniu ruchu mapy.
- Liczba markerów powinna mieć limit.
- Limit powinien być możliwy do zmiany przez Remote Config.

## Markery

Marker powinien przekazywać:

- kategorię miejsca,
- podstawowy status miejsca,
- możliwość otwarcia szczegółów,
- jasne powiązanie z bottom sheetem.

Przy większej liczbie miejsc stosujemy clustering.

## Bottom sheet

Bottom sheet po kliknięciu markera powinien zawierać:

- nazwę miejsca,
- kategorię,
- ocenę,
- adres albo dystans,
- krótkie CTA do szczegółów,
- CTA do Google Maps, jeśli dostępne.

## Stany mapy

Mapa musi obsługiwać:

- loading,
- brak lokalizacji,
- odmowę uprawnień,
- brak internetu,
- brak miejsc w obszarze,
- błąd Google Maps,
- błąd pobierania danych.

## Accessibility

- Dane z mapy muszą mieć alternatywę listową.
- Markery i akcje muszą mieć opisy dla TalkBack.
- Brak lokalizacji nie może blokować całej aplikacji.
- Użytkownik musi móc korzystać z listy bez mapy.

## Performance checklist

- [ ] Mapa nie pobiera wszystkich miejsc.
- [ ] Requesty są ograniczone przez debounce/throttle.
- [ ] Markery mają limit.
- [ ] Clustering działa dla większej liczby markerów.
- [ ] Bottom sheet nie resetuje się niepotrzebnie.
- [ ] Ruch mapy nie powoduje lawiny rekompozycji.
- [ ] Brak lokalizacji ma czytelny fallback.

## Release checklist

- [ ] Mapa działa z lokalizacją.
- [ ] Mapa działa bez lokalizacji.
- [ ] Brak internetu nie powoduje crasha.
- [ ] Markery klikają się poprawnie.
- [ ] Bottom sheet pokazuje właściwe miejsce.
- [ ] Google Maps API key działa w release buildzie.
- [ ] Klucz Maps jest ograniczony do właściwego package name i SHA.
