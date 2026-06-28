# Android permissions and Google Play compliance

Powiązane issue: #274  
Powiązane issue release: #269, #275

Ostatnia aktualizacja: 2026-06-28

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
| `READ_MEDIA_IMAGES` | Do decyzji przed release | Zadeklarowane w manifeście; główne flow zdjęć używa Android Photo Picker. |
| `POST_NOTIFICATIONS` | Runtime Android 13+ | Powiadomienia push Firebase Cloud Messaging. |

Manifest nie deklaruje:

- `ACCESS_BACKGROUND_LOCATION`,
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
| Zdjęcia | Główne flow używa Android Photo Picker. | Czy system nie pokazuje szerokiego dostępu do całej galerii tam, gdzie wystarczy picker. |

## Google Play Console

Rekomendowane deklaracje:

- Lokalizacja: tylko podczas używania aplikacji, brak lokalizacji w tle.
- Kamera: opcjonalna, do dodawania zdjęć.
- Zdjęcia: użytkownik wybiera zdjęcia do publikacji; główne flow powinno działać przez Photo Picker.
- Powiadomienia: opcjonalne, do aktywności społecznościowej i informacji o koncie.
- Dane dzieci: aplikacja jest dla rodziców/opiekunów, nie jest kierowana bezpośrednio do dzieci.

## Decyzja o `READ_MEDIA_IMAGES`

Przed publikacją trzeba podjąć jedną z decyzji:

1. Usunąć `READ_MEDIA_IMAGES`, jeśli wszystkie obsługiwane flow zdjęć działają przez Android Photo Picker bez szerokiego dostępu do galerii.
2. Zostawić `READ_MEDIA_IMAGES`, jeśli istnieje realny flow wymagający dostępu do biblioteki zdjęć poza Photo Pickerem.

Aktualna rekomendacja:

```text
Zweryfikować na Androidzie 13+ i 14+, czy usunięcie READ_MEDIA_IMAGES nie psuje avatara,
dodawania zdjęć miejsc, zdjęć opinii i edycji profilu. Jeśli testy przejdą, usunąć uprawnienie
osobnym PR-em przed publikacją.
```

## Manual QA przed zamknięciem #274

- [ ] Android 13+ notification permission: allow / deny.
- [ ] Location permission: precise / approximate / deny.
- [ ] Camera permission: allow / deny.
- [ ] Photo Picker: avatar, zdjęcie miejsca, zdjęcie opinii.
- [ ] Aplikacja działa bez lokalizacji.
- [ ] Aplikacja działa bez powiadomień.
- [ ] Aplikacja działa bez kamery, jeśli użytkownik wybiera zdjęcie z galerii.
- [ ] Google Play Console nie deklaruje background location.
- [ ] Decyzja o `READ_MEDIA_IMAGES` zapisana w issue #274.

## Kryteria zamknięcia

Issue #274 można zamknąć po ręcznym teście runtime permissions i finalnej decyzji
o `READ_MEDIA_IMAGES`. Sam ten dokument przygotowuje audyt, ale nie zastępuje testu
na urządzeniu i potwierdzenia w Google Play Console.
