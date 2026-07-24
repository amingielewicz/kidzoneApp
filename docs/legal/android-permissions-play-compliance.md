# Android permissions and Google Play compliance

Powiązane issue: #274, #303  
Powiązane issue release: #269, #275

Ostatnia aktualizacja: 2026-07-13

## Cel

Dokument opisuje aktualny stan uprawnień Androida, zachowanie aplikacji po odmowie zgody oraz informacje potrzebne do deklaracji w Google Play Console.

## Manifest

Aktualny manifest `app/src/main/AndroidManifest.xml` deklaruje:

| Uprawnienie | Status | Użycie |
| --- | --- | --- |
| `INTERNET` | wymagane | Firebase, Google Maps, Google Play Services i komunikacja sieciowa |
| `ACCESS_NETWORK_STATE` | wymagane | wykrywanie online/offline i synchronizacja |
| `ACCESS_FINE_LOCATION` | runtime | dokładna lokalizacja na mapie, sortowanie po odległości i dodawanie miejsca |
| `ACCESS_COARSE_LOCATION` | runtime | lokalizacja przybliżona |
| `CAMERA` | runtime | wykonanie zdjęcia w aplikacji |
| `POST_NOTIFICATIONS` | runtime na Androidzie 13+ | powiadomienia push FCM |

Manifest nie deklaruje:

- `ACCESS_BACKGROUND_LOCATION`,
- `READ_MEDIA_IMAGES`,
- `READ_EXTERNAL_STORAGE`,
- uprawnień do audio i wideo,
- uprawnień SMS, kontaktów, kalendarza i telefonu,
- `QUERY_ALL_PACKAGES`.

## Funkcje sprzętowe

| Funkcja | Status |
| --- | --- |
| `android.hardware.camera` | `required="false"` |
| `android.hardware.camera.autofocus` | `required="false"` |
| `android.hardware.location` | `required="false"` |

Aplikacja działa bez aparatu i bez zgody na lokalizację, chociaż część funkcji jest wtedy ograniczona.

## Wspólna obsługa runtime permissions

Obsługa lokalizacji i kamery jest wydzielona do wspólnych handlerów używanych przez ekrany, które uruchamiają te funkcje.

### Lokalizacja

Wspólna funkcja `requestLocationPermissionOrOpenSettings` obsługuje dwa scenariusze:

1. Gdy system może jeszcze pokazać dialog, aplikacja uruchamia prośbę o uprawnienie.
2. Gdy użytkownik trwale odmówił zgody, aplikacja otwiera ustawienia aplikacji.

Mechanizm powinien być stosowany we wszystkich punktach wejścia do lokalizacji, między innymi:

- przycisk lokalizacji na mapie,
- ekran startowy i akcja „włącz lokalizację”,
- lista miejsc i sortowanie „od najbliższych”,
- dodawanie lub korekta lokalizacji miejsca.

Dzięki temu zachowanie po kolejnych odmowach jest spójne. Aplikacja nie pozostawia przycisku w stanie, w którym kolejne kliknięcia nic nie robią.

### Kamera

Kamera korzysta ze wspólnego handlera uprawnienia. Po trwałej odmowie użytkownik jest kierowany do ustawień aplikacji. Brak zgody na kamerę nie blokuje wyboru zdjęcia przez systemowy Photo Picker.

### Powiadomienia

Na Androidzie 13+ aplikacja prosi o `POST_NOTIFICATIONS`. Odmowa nie blokuje pozostałych funkcji.

## Zdjęcia i Android Photo Picker

Aplikacja nie deklaruje szerokiego dostępu do galerii.

Główne przepływy korzystają z `ActivityResultContracts.PickVisualMedia` lub `PickMultipleVisualMedia`:

- avatar użytkownika,
- zdjęcia miejsca,
- zdjęcia opinii,
- zdjęcie dodawane w szczegółach miejsca.

Efekt dla Google Play:

```text
Aplikacja nie deklaruje READ_MEDIA_IMAGES ani READ_EXTERNAL_STORAGE.
Użytkownik wybiera konkretne zdjęcia przez systemowy Android Photo Picker.
```

## Google Play Console

Rekomendowane deklaracje:

- lokalizacja: tylko podczas używania aplikacji,
- brak lokalizacji w tle,
- kamera: opcjonalna, używana do wykonania zdjęcia,
- zdjęcia: wybór konkretnych plików przez systemowy Photo Picker,
- powiadomienia: opcjonalne,
- grupa docelowa: rodzice i opiekunowie, nie dzieci.

## Manual QA

Szczegółowa macierz urządzeń:

`docs/qa/android-permissions-device-matrix.md`

### Lokalizacja

- [ ] pierwsza prośba o zgodę,
- [ ] zgoda dokładna,
- [ ] zgoda przybliżona,
- [ ] pierwsza odmowa,
- [ ] kolejna odmowa,
- [ ] trwała odmowa i przekierowanie do ustawień aplikacji,
- [ ] powrót z ustawień po nadaniu zgody,
- [ ] wyłączona usługa GPS przy nadanym uprawnieniu,
- [ ] spójne zachowanie na mapie, ekranie startowym, liście i formularzu miejsca.

### Kamera

- [ ] zgoda,
- [ ] odmowa,
- [ ] trwała odmowa i przekierowanie do ustawień,
- [ ] działanie Photo Pickera bez zgody na kamerę.

### Zdjęcia

- [ ] avatar otwiera Photo Picker,
- [ ] zdjęcia miejsca otwierają Photo Picker,
- [ ] zdjęcia opinii otwierają Photo Picker,
- [ ] brak dialogu o szerokim dostępie do galerii na Androidzie 13 i 14+.

### Powiadomienia

- [ ] zgoda na Androidzie 13+,
- [ ] odmowa,
- [ ] aplikacja działa poprawnie bez zgody.

## Kryteria zamknięcia

Dokumentację można uznać za aktualną po:

1. przejściu macierzy testów na fizycznym urządzeniu,
2. potwierdzeniu spójnego przekierowania do ustawień po trwałej odmowie,
3. potwierdzeniu braku `READ_MEDIA_IMAGES` i `READ_EXTERNAL_STORAGE` w finalnym manifeście,
4. zgodności deklaracji Data Safety i sekcji uprawnień w Google Play Console z zachowaniem aplikacji.
