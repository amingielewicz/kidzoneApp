# Android permissions and Google Play compliance

Powiązane issue: #274, #303  
Powiązane issue release: #269, #275

Ostatnia aktualizacja: 2026-06-29

## Cel

Dokument zbiera stan uprawnień Androida przed publikacją kidZone w Google Play.
Ma pomóc przepisać deklaracje do Play Console i wykonać ręczny test zgód na urządzeniu.

## Manifest

Aktualny manifest `app/src/main/AndroidManifest.xml` deklaruje:

| Uprawnienie | Status | Użycie |
| --- | --- | --- |
| `INTERNET` | Wymagane | Firebase, Google Maps, Google Play Services, API sieciowe. |
| `ACCESS_NETWORK_STATE` | Wymagane | Detekcja online/offline i zachowanie synchronizacji. |
| `ACCESS_FINE_LOCATION` | Runtime | Mapa, miejsca w pobliżu, pobranie lokalizacji przy dodawaniu miejsca. |
| `ACCESS_COARSE_LOCATION` | Runtime | Przybliżona lokalizacja, gdy system/użytkownik ograniczy dokładność. |
| `CAMERA` | Runtime | Zrobienie zdjęcia w aplikacji. Kamera jest funkcją opcjonalną. |
| `POST_NOTIFICATIONS` | Runtime Android 13+ | Powiadomienia push Firebase Cloud Messaging. |

Manifest nie deklaruje:

- `ACCESS_BACKGROUND_LOCATION`,
- `READ_MEDIA_IMAGES`,
- `READ_EXTERNAL_STORAGE`,
- uprawnień do audio/wideo,
- uprawnień SMS/kontaktów/kalendarza/telefonu,
- `QUERY_ALL_PACKAGES`.

## Funkcje sprzętowe

| Funkcja | Status |
| --- | --- |
| `android.hardware.camera` | `required="false"` |
| `android.hardware.camera.autofocus` | `required="false"` |
| `android.hardware.location` | `required="false"` |

To jest właściwe dla aplikacji, która może działać bez aparatu i bez zgody lokalizacji,
choć część funkcji będzie ograniczona.

## Runtime permissions

| Obszar | Status w aplikacji | Co sprawdzić ręcznie |
| --- | --- | --- |
| Lokalizacja | Aplikacja pokazuje contextual rationale przed systemowym dialogiem. | Odmowa, zgoda approximate, zgoda precise, ponowna próba po odmowie. |
| Kamera | Kamera jest osobnym flow i wymaga runtime permission. | Pierwsza zgoda, odmowa, odmowa z "nie pytaj ponownie", działanie bez kamery. |
| Powiadomienia | Android 13+ używa `POST_NOTIFICATIONS` i rationale. | Zgoda, odmowa, zachowanie ustawień powiadomień po odmowie. |
| Zdjęcia | Główne flow używa Android Photo Picker i nie deklaruje `READ_MEDIA_IMAGES`. | Avatar, zdjęcie miejsca i zdjęcie opinii powinny otwierać picker bez systemowej prośby o szeroki dostęp do galerii. |

## Google Play Console

Rekomendowane deklaracje:

- Lokalizacja: tylko podczas używania aplikacji, brak lokalizacji w tle.
- Kamera: opcjonalna, do dodawania zdjęć.
- Zdjęcia: użytkownik wybiera zdjęcia do publikacji; główne flow powinno działać przez Photo Picker.
- Powiadomienia: opcjonalne, do aktywności społecznościowej i informacji o koncie.
- Dane dzieci: aplikacja jest dla rodziców/opiekunów, nie jest kierowana bezpośrednio do dzieci.

## Decyzja o `READ_MEDIA_IMAGES`

Decyzja na 2026-06-29:

```text
Usuwamy READ_MEDIA_IMAGES z manifestu.
```

Uzasadnienie:

- avatar używa `ActivityResultContracts.PickVisualMedia`,
- dodawanie miejsca używa `ActivityResultContracts.PickMultipleVisualMedia`,
- dodawanie opinii używa `ActivityResultContracts.PickMultipleVisualMedia`,
- dodawanie zdjęcia w szczegółach miejsca używa `ActivityResultContracts.PickVisualMedia`,
- kamera ma osobne runtime permission `CAMERA`,
- w kodzie nie ma użycia `READ_MEDIA_IMAGES`, `READ_EXTERNAL_STORAGE`, `GetContent` ani `OpenDocument`.

Efekt dla Google Play:

```text
Aplikacja nie deklaruje szerokiego dostępu do galerii. Użytkownik wybiera konkretne zdjęcia
przez systemowy Android Photo Picker.
```

## Manual QA przed zamknięciem #274

Szczegółowa macierz urządzeń i szablon komentarza PASS/FAIL:

```text
docs/qa/android-permissions-device-matrix.md
```

- [ ] Android 13+ notification permission: allow / deny.
- [ ] Location permission: precise / approximate / deny.
- [ ] Camera permission: allow / deny.
- [ ] Photo Picker: avatar, zdjęcie miejsca, zdjęcie opinii.
- [ ] Android 13+/14+: brak systemowego dialogu o szerokim dostępie do zdjęć/galerii.
- [ ] Aplikacja działa bez lokalizacji.
- [ ] Aplikacja działa bez powiadomień.
- [ ] Aplikacja działa bez kamery, jeśli użytkownik wybiera zdjęcie z galerii.
- [ ] Google Play Console nie deklaruje background location.
- [x] Decyzja o `READ_MEDIA_IMAGES`: usunięte z manifestu, bo Photo Picker wystarcza.

## Kryteria zamknięcia

Issue #274 można zamknąć po ręcznym teście runtime permissions i potwierdzeniu
w Google Play Console. Decyzja o `READ_MEDIA_IMAGES` jest już wykonana w repo,
ale dokument nie zastępuje testu na urządzeniu.
