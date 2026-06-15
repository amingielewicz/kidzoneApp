# Firestore scalability dataset

Powiązane issue: #179

## Cel

Wygenerowanie powtarzalnego zestawu danych do walidacji wydajności i skalowalności aplikacji kidZone.

## Generowanie danych

Instalacja:

```bash
npm install
```

100 miejsc:

```bash
npm run generate:100
```

1000 miejsc:

```bash
npm run generate:1000
```

Generator tworzy:

- users
- places
- reviews

Plik wynikowy trafia do katalogu output.

## Dry run

Bez zapisu do Firestore:

```bash
node seed-firestore.mjs --input=output/kidzone-scalability-100.json --dryRun=true
```

## Seed Firestore

Wymagane:

- konto serwisowe lub ADC
- dostęp do projektu Firebase

Przykład:

```bash
node seed-firestore.mjs \
  --input=output/kidzone-scalability-1000.json \
  --projectId=<firebase-project-id>
```

## Walidacja

Po załadowaniu danych sprawdzić:

- ekran listy miejsc,
- mapę,
- ranking miejsc,
- ranking użytkowników,
- profil użytkownika,
- wyszukiwarkę.

## Raport

Zanotować:

- czas ładowania listy,
- czas ładowania mapy,
- płynność przewijania,
- zachowanie paginacji,
- zachowanie markerów,
- liczbę odczytów Firestore.
