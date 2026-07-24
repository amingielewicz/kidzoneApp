# Performance, Security and Testing Guidelines

Ostatnia aktualizacja: 2026-07-13

## Cel

Minimalne standardy jakości technicznej kidZone obejmujące wydajność, bezpieczeństwo, testowalność i bramki release.

## Performance

### Dane

- listy używają limitów i paginacji,
- wyszukiwanie i ruch mapy mają debounce lub throttle,
- repository ukrywa cache i źródła danych,
- karty nie wykonują osobnych requestów po dane agregowane,
- pola takie jak `ratingAverage` i `reviewsCount` są wykorzystywane zamiast pobierania pełnych opinii,
- zapis i retry są idempotentne,
- operacje offline nie udają sukcesu.

### Compose

- listy używają `LazyColumn` i `LazyRow`,
- elementy mają stabilne `key`,
- ciężkie mapowanie, sortowanie i filtrowanie jest poza Composable,
- stan jest niemutowalny,
- efekty uboczne mają stabilne klucze,
- recomposition nie uruchamia ponownie dialogów ani requestów,
- duże ekrany są sprawdzane pod kątem jank i czasu renderowania.

### Mapa

- dane są pobierane dla bounds lub promienia,
- liczba markerów jest ograniczona,
- clustering działa dla większych zbiorów,
- mapa ma fallback do listy,
- dokładna lokalizacja nie trafia do telemetryki,
- usage i koszty są monitorowane.

### Zdjęcia

- walidujemy MIME, rozmiar i liczbę plików,
- zdjęcia są kompresowane,
- EXIF/GPS są usuwane, jeśli nie są potrzebne,
- upload ma timeout, retry i kontrolowany błąd,
- Photo Picker działa bez szerokiego dostępu do galerii.

## Security

### Firebase

- Firestore Rules i Storage Rules blokują nieuprawniony dostęp,
- Cloud Functions sprawdzają auth, role i payload,
- App Check release używa Play Integrity,
- pola administracyjne nie są edytowalne z klienta,
- tokeny FCM są prywatne,
- operacje account deletion są spójne w Auth, Firestore, Storage i FCM.

### Logging i telemetryka

Release nie loguje:

- haseł i tokenów,
- e-maili i danych profilu,
- dokładnej lokalizacji,
- treści formularzy i opinii,
- URI zdjęć,
- pełnych payloadów.

Crashlytics, Analytics i Performance muszą odpowiadać Data Safety.

### Uprawnienia

- brak `ACCESS_BACKGROUND_LOCATION`,
- brak `READ_MEDIA_IMAGES` i `READ_EXTERNAL_STORAGE`,
- trwała odmowa lokalizacji i kamery prowadzi do ustawień,
- odmowa nie tworzy martwych przycisków,
- aplikacja działa bez zgód opcjonalnych.

## Testing

### Minimalny zestaw

- unit tests dla logiki domenowej, mapperów i walidacji,
- ViewModel tests dla stanów i eventów,
- Rules tests dla Firestore i Storage,
- build i lint dla Cloud Functions i panelu admina,
- manual smoke na urządzeniu,
- manual regression przed release,
- performance i load checks dla zmian kosztowych lub wydajnościowych.

### Krytyczne flow

- rejestracja, logowanie i logout,
- Start, Mapa i Lista,
- szczegóły miejsca,
- dodawanie miejsca, opinii i zdjęć,
- profil i ranking,
- runtime permissions,
- offline i cache,
- account deletion,
- widget,
- deep linki i powiadomienia.

### Scenariusze negatywne

- brak internetu,
- timeout,
- trwała odmowa uprawnienia,
- wyłączony GPS,
- błąd Rules,
- wygasła sesja,
- częściowy cleanup danych,
- double submit,
- race condition,
- nieprawidłowy deep link.

## Release quality gate

Release jest NO-GO, jeśli:

- signed AAB nie przechodzi,
- występuje crash przy starcie,
- logowanie albo podstawowe flow nie działa,
- Data Safety nie odpowiada buildowi,
- account deletion ma FAIL,
- runtime permissions mają FAIL,
- Rules albo App Check nie są zweryfikowane,
- release loguje dane wrażliwe,
- widget lub cache ujawnia prywatne dane.

## Checklista

- [ ] limity, paginacja i debounce działają,
- [ ] mapa nie wykonuje lawiny requestów,
- [ ] zdjęcia mają walidację i kompresję,
- [ ] Rules i App Check są sprawdzone,
- [ ] telemetryka nie zawiera PII,
- [ ] testy negatywne pokrywają krytyczne flow,
- [ ] runtime permissions i account deletion mają PASS,
- [ ] manual smoke przeszedł,
- [ ] monitoring rollout jest przygotowany.
