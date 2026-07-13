# Maps Handbook

Ostatnia aktualizacja: 2026-07-13

## Cel

Zasady użycia Google Maps w kidZone z naciskiem na dostępność, wydajność, koszty i spójne zachowanie uprawnień.

## Rola mapy

Mapa służy do:

- odkrywania miejsc w pobliżu,
- orientacji przestrzennej,
- wyboru miejsca,
- przejścia do szczegółów,
- otwarcia zewnętrznej nawigacji.

Mapa nie jest jedynym sposobem dostępu do danych. Lista miejsc pozostaje pełnym fallbackiem.

## Ładowanie danych

- nie pobieramy całej kolekcji,
- używamy bounds lub promienia,
- request następuje po ustabilizowaniu ruchu mapy,
- liczba markerów ma limit,
- limit i debounce mogą być sterowane przez Remote Config,
- zmiana kamery nie może wywoływać lawiny requestów,
- wynik jest cache'owany tam, gdzie ma to sens.

## Markery i clustering

Marker przekazuje kategorię, status i możliwość otwarcia miejsca. Przy większej liczbie punktów używamy clusteringu.

Kliknięcie markera powinno wybrać miejsce w sposób stabilny i nie resetować niepotrzebnie kamery ani bottom sheeta.

## Bottom sheet

Powinien zawierać:

- nazwę,
- kategorię,
- ocenę,
- adres lub dystans,
- CTA do szczegółów,
- CTA do zewnętrznej nawigacji.

Komponenty kategorii powinny być spójne z listą i ekranem Start.

## Lokalizacja użytkownika

Wspólny handler obsługuje:

- pierwszą prośbę,
- odmowę,
- trwałą odmowę,
- przekierowanie do ustawień aplikacji,
- powrót z ustawień,
- wyłączony GPS.

Brak zgody nie blokuje mapy ani listy. Po trwałej odmowie akcja „Moja lokalizacja” nie może stać się martwa.

## Stany

Mapa obsługuje:

- loading,
- brak zgody,
- wyłączoną usługę lokalizacji,
- brak internetu,
- brak miejsc,
- błąd Google Maps,
- błąd pobierania danych,
- pusty lub częściowy cache.

## Accessibility

- lista jest alternatywą dla markerów,
- akcje mają opisy TalkBack,
- nie polegamy wyłącznie na kolorze,
- brak lokalizacji nie blokuje aplikacji,
- bottom sheet ma logiczną kolejność fokusu.

## Security i koszty

- klucz Maps jest ograniczony do package name i SHA,
- aktywne są tylko potrzebne API,
- usage i billing są monitorowane,
- limity chronią przed kosztownymi zapytaniami,
- dokładna lokalizacja nie trafia do logów ani Analytics.

## Checklista

- [ ] mapa nie pobiera wszystkich miejsc,
- [ ] requesty mają debounce/throttle,
- [ ] clustering działa,
- [ ] limity są kontrolowane,
- [ ] lista działa bez mapy i lokalizacji,
- [ ] trwała odmowa prowadzi do ustawień,
- [ ] powrót z ustawień odświeża stan,
- [ ] ruch mapy nie powoduje lawiny recomposition,
- [ ] klucz API jest ograniczony,
- [ ] brak internetu nie powoduje crasha.
