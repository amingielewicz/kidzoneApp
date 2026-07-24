# Firestore Indexes

Ostatnia aktualizacja: 2026-07-13

## Cel

Dokument opisuje zasady utrzymania indeksów Firestore wymaganych przez zapytania kidZone.

## Zasady

- indeks wynika z realnego zapytania,
- nowe zapytanie ma limit i paginację,
- indeksy są wersjonowane w `firestore.indexes.json`,
- indeks jest wdrażany przed buildem aplikacji zależnym od zapytania,
- brak indeksu nie może zostać odkryty dopiero przez użytkowników produkcyjnych,
- usunięcie indeksu wymaga potwierdzenia, że żaden aktywny build go nie używa.

## Typowe zapytania

### Miejsca według kategorii i oceny

```text
places
category ascending
status ascending
ratingAverage descending
reviewsCount descending
```

### Najnowsze miejsca i moderacja

```text
places
status ascending
createdAt descending
```

### Ranking miejsc

```text
places
status ascending
ratingAverage descending
reviewsCount descending
```

### Opinie miejsca

```text
reviews
placeId ascending
status ascending
createdAt descending
```

### Opinie użytkownika

```text
reviews
userId ascending
status ascending
createdAt descending
```

### Zgłoszenia

```text
reports
status ascending
createdAt descending
```

Opcjonalnie także `targetType`, jeśli panel filtruje po typie zasobu.

### Propozycje zmian

```text
changeRequests
status ascending
createdAt descending
```

### Powiadomienia użytkownika

```text
notifications
userId ascending
createdAt descending
```

lub z filtrem statusu przeczytania, jeśli takie zapytanie istnieje.

## Geo queries

Dla mapy używamy bounds, promienia albo geohash.

- nie pobieramy całej kolekcji,
- wynik ma limit,
- ruch mapy ma debounce,
- geohash query może wymagać kilku zakresów i deduplikacji wyników,
- clustering odbywa się po stronie klienta dla ograniczonego zbioru,
- indeksy muszą odpowiadać faktycznym polom statusu i geohash.

## Proces dodania zapytania

1. Zapisz cel biznesowy i oczekiwany limit.
2. Sprawdź query plan i wymagany indeks.
3. Dodaj indeks do `firestore.indexes.json`.
4. Wdróż do projektu testowego.
5. Przetestuj dane puste, małe i większe.
6. Sprawdź koszt odczytów.
7. Wdróż indeks do produkcji przed aplikacją.
8. Zweryfikuj brak błędów `failed-precondition`.

## Koszty

Indeks nie zastępuje limitów. Zapytania powinny:

- używać selektywnych filtrów,
- unikać pełnych skanów,
- pobierać tylko potrzebną stronę,
- wykorzystywać pola agregowane,
- nie wykonywać N+1 reads dla kart listy.

## Testy

- poprawne sortowanie,
- paginacja bez duplikatów i braków,
- zmiana filtrów,
- puste wyniki,
- brak indeksu w projekcie testowym daje kontrolowany błąd,
- ranking i panel administracyjny działają po deployu,
- geo query nie zwraca nieograniczonego zbioru.

## Checklista PR

- [ ] nowe zapytanie ma limit,
- [ ] nie pobiera całej kolekcji,
- [ ] wymagany indeks jest wersjonowany,
- [ ] paginacja jest stabilna,
- [ ] koszt został oceniony,
- [ ] test na projekcie testowym przeszedł,
- [ ] release uwzględnia kolejność deploy indeksu przed aplikacją.

## Checklista release

- [ ] `firestore.indexes.json` jest aktualny,
- [ ] indeksy są wdrożone do właściwego projektu,
- [ ] listy, ranking, mapa i panel admina działają,
- [ ] brak błędów indeksów w logach,
- [ ] aktywne buildy nie zależą od usuwanego indeksu.
