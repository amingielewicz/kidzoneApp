# Firestore Indexes

## Cel

Dokument opisuje indeksy Firestore potrzebne w KidZone oraz zasady ich utrzymania.

## Zasady

- Kazde zapytanie z filtrowaniem i sortowaniem moze wymagac indeksu zlozonego.
- Kazde zapytanie rankingowe powinno miec opisany wymagany indeks.
- Nie tworzymy indeksow na zapas bez realnego zapytania.
- Indeksy powinny byc wersjonowane w `firestore.indexes.json`.
- Brak indeksu na produkcji moze zablokowac flow uzytkownika.

## Typowe indeksy

### Lista miejsc po kategorii i ocenie

Cel:

- ekran listy miejsc,
- filtrowanie po kategorii,
- sortowanie po ocenie i liczbie opinii.

Pola:

```text
places
category ascending
ratingAverage descending
reviewsCount descending
```

### Lista miejsc po statusie i czasie utworzenia

Cel:

- najnowsze miejsca,
- panel administracyjny,
- moderacja.

Pola:

```text
places
status ascending
createdAt descending
```

### Ranking miejsc

Cel:

- TOP miejsc,
- ranking publiczny.

Pola:

```text
places
status ascending
ratingAverage descending
reviewsCount descending
```

### Opinie miejsca

Cel:

- szczegoly miejsca,
- lista opinii.

Pola:

```text
reviews
placeId ascending
status ascending
createdAt descending
```

### Opinie uzytkownika

Cel:

- profil uzytkownika,
- moje opinie.

Pola:

```text
reviews
userId ascending
createdAt descending
```

### Zgloszenia administracyjne

Cel:

- panel admina,
- kolejka moderacji.

Pola:

```text
reports
status ascending
createdAt descending
```

### Propozycje zmian

Cel:

- panel admina,
- moderacja zmian.

Pola:

```text
changeRequests
status ascending
createdAt descending
```

## Geo queries

Dla mapy uzywamy podejscia opartego o bounds, promien albo geohash.

Zasady:

- nie pobieramy calej kolekcji `places`,
- request jest ograniczony przez bounds albo promien,
- limit markerow powinien byc sterowany przez Remote Config,
- clustering jest wymagany przy wiekszej liczbie wynikow.

## Utrzymanie indeksow

Przy dodaniu nowego zapytania:

1. Sprawdz, czy wymaga indeksu zlozonego.
2. Dodaj indeks do `firestore.indexes.json`.
3. Wdroz indeks na staging.
4. Przetestuj flow.
5. Wdroz indeks na produkcje przed releasem aplikacji.

## Checklist PR

- [ ] Nowe zapytanie ma limit.
- [ ] Nowe zapytanie nie pobiera calej kolekcji.
- [ ] Indeks zostal dodany do `firestore.indexes.json`, jesli jest wymagany.
- [ ] Zapytanie dziala na staging.
- [ ] Release checklist uwzglednia wdrozenie indeksu.

## Checklist release

- [ ] `firestore.indexes.json` jest aktualny.
- [ ] Indeksy sa wdrozone na wlasciwy projekt Firebase.
- [ ] Zapytania list dzialaja bez bledow indeksu.
- [ ] Ranking dziala bez bledow indeksu.
- [ ] Panel admina dziala bez bledow indeksu.
