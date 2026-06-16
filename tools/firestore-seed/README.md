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
- placeholderowe `photoUrls` dla wszystkich miejsc i opinii
- jeden kontrolowany błędny URL zdjęcia, żeby sprawdzić error state ładowania obrazka

Plik wynikowy trafia do katalogu output.

Zdjęcia są domyślnie włączone dla wszystkich miejsc i wszystkich opinii. Można je wyłączyć albo zmienić gęstość generowania:

```bash
node generate-scalability-data.mjs \
  --places=1000 \
  --photos=true \
  --placePhotoEvery=1 \
  --reviewPhotoEvery=1 \
  --brokenPhoto=true
```

Używane są zewnętrzne placeholdery URL (`picsum.photos`), bez uploadu plików do Firebase Storage.

## Walidacja zdjęć w pliku

```bash
npm run validate -- --input=output/kidzone-scalability-100.json
```

Walidator sprawdza, czy:

- każde miejsce ma minimum jeden `photoUrl`,
- każda opinia ma minimum jeden `photoUrl`,
- miejsca z `photoUrls` mają kompletne `photoUploadedBy`,
- liczba `photoHashes` zgadza się z liczbą `photoUrls`,
- w danych jest co najmniej jeden błędny URL do testu placeholder/error state.

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
- listę miejsc ze zdjęciami,
- ekran szczegółów miejsca ze zdjęciami miejsca,
- galerie i miniatury opinii,
- placeholder oraz error state dla błędnego URL,
- mapę,
- ranking miejsc,
- ranking użytkowników,
- profil użytkownika,
- wyszukiwarkę.

## Firebase Storage: koszt i ryzyko

Ten generator celowo nie uploaduje prawdziwych plików do Firebase Storage. Realny upload powinien być osobnym etapem, bo wymaga:

- policzenia kosztów transferu i przechowywania danych,
- reguł Firebase Storage dopasowanych do testowych ścieżek,
- mechanizmu sprzątania plików po seedzie,
- limitów bezpieczeństwa, żeby przypadkiem nie wygenerować tysięcy dużych plików,
- decyzji, czy dane testowe mogą używać publicznych URL-i, czy muszą przechodzić przez produkcyjny upload.

## Raport

Zanotować:

- czas ładowania listy,
- czas ładowania mapy,
- płynność przewijania,
- zachowanie paginacji,
- zachowanie markerów,
- liczbę odczytów Firestore.
