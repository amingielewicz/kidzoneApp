# Performance, Security and Testing Guidelines

## Cel

Ten dokument zbiera minimalne standardy jakości technicznej dla KidZone: wydajność, bezpieczeństwo i testy.

## Performance

### Data loading

- Nie pobieramy całej kolekcji miejsc bez potrzeby.
- Lista miejsc musi mieć limit i paginację.
- Wyszukiwarka musi mieć debounce.
- Ranking korzysta z gotowych pól, np. `ratingAverage` i `reviewsCount`.
- Karty miejsc nie robią osobnych requestów po opinie.
- Repository jest jednym źródłem prawdy.

### Compose

- Używamy `LazyColumn` / `LazyRow` dla list.
- Elementy list mają stabilne `key`.
- Ciężkie filtrowanie i sortowanie nie siedzi w Composable.
- Stan ekranu jest w ViewModelu.

### Mapa

- Mapa pobiera miejsca dla obszaru, bounds albo promienia.
- Nie ładujemy wszystkich markerów naraz.
- Ruch mapy ma debounce/throttle.
- Limit markerów powinien być kontrolowany konfiguracją.
- Dla dużych danych używamy clusteringu.

## Security

### Firebase

- Firestore Rules muszą blokować nieautoryzowany dostęp.
- Storage Rules muszą walidować właściciela i typ operacji.
- Cloud Functions muszą mieć auth check.
- App Check powinien być zweryfikowany przed release.

### Logging

- Release nie loguje danych wrażliwych.
- Nie logujemy e-maili, lokalizacji, treści formularzy ani identyfikatorów sesji.
- Debug logging nie może przypadkiem wejść do release.

### Upload zdjęć

- Walidujemy typ pliku.
- Walidujemy rozmiar pliku.
- Kompresujemy zdjęcia.
- Czyścimy EXIF/GPS, jeśli nie jest potrzebny.

## Testing

### Minimalny zestaw

- Unit tests dla logiki domenowej.
- ViewModel tests dla stanów UI.
- Smoke test release builda.
- Manual regression przed release.
- Firestore Rules tests.
- Cloud Functions tests.

### Krytyczne flow

- Login.
- Rejestracja.
- Mapa.
- Lista miejsc.
- Dodawanie miejsca.
- Opinie i oceny.
- Ranking.
- Profil.
- Brak internetu.
- Brak lokalizacji.

## Release quality gate

Release nie idzie dalej, jeśli:

- występuje crash przy starcie,
- login nie działa,
- mapa/lista nie działa,
- dodawanie miejsca nie działa,
- release loguje dane wrażliwe,
- Crashlytics nie działa,
- signed build nie przechodzi.

## Checklist

- [ ] Limity i paginacja działają.
- [ ] Debounce wyszukiwarki działa.
- [ ] Mapa nie wykonuje lawiny requestów.
- [ ] Release nie loguje danych wrażliwych.
- [ ] Rules są sprawdzone.
- [ ] Smoke test przeszedł.
- [ ] Crashlytics zbiera testowy crash.
