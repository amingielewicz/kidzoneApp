# Android permissions and device compatibility matrix

Powiązane issue: #274, #303

Ostatnia aktualizacja: 2026-07-13

## Cel

Macierz służy do ręcznego sprawdzenia runtime permissions, Android Photo Pickera i zachowania aplikacji po odmowie zgód.

## Dane testu

| Pole | Wartość |
| --- | --- |
| Commit / tag | |
| Build type | debug / release / internal |
| Firebase project | |
| Urządzenie | |
| Android | |
| Tester | |
| Data | |

## Minimalna macierz urządzeń

| Android | Urządzenie | Status | Zakres |
| --- | --- | --- | --- |
| 13 | fizyczne lub emulator | TODO | powiadomienia runtime, Photo Picker, odmowy |
| 14 | fizyczne lub emulator | TODO | Photo Picker, trwałe odmowy, ustawienia aplikacji |
| 15 | fizyczne lub emulator | TODO | smoke test zgodności |
| 16 | fizyczne lub emulator, jeśli dostępne | TODO | regresja runtime permissions |

Warto dodać co najmniej jedno urządzenie producenta z mocno zmodyfikowanym Androidem, na przykład Xiaomi, Samsung lub realme.

## Lokalizacja

Każdy punkt wejścia do lokalizacji powinien korzystać ze wspólnego mechanizmu `requestLocationPermissionOrOpenSettings`.

| Punkt wejścia | Pierwsza odmowa | Kolejna odmowa | Trwała odmowa | Wynik |
| --- | --- | --- | --- | --- |
| Start: „Włącz lokalizację” | brak crasha, czytelny komunikat | ponowna obsługa akcji | ustawienia aplikacji | TODO |
| Lista: sortowanie „Od najbliższych” | brak crasha, lista nadal działa | ponowna obsługa akcji | ustawienia aplikacji | TODO |
| Mapa: „Moja lokalizacja” | brak crasha, mapa nadal działa | ponowna obsługa akcji | ustawienia aplikacji | TODO |
| Dodawanie miejsca | formularz pozostaje dostępny | ponowna obsługa akcji | ustawienia aplikacji | TODO |
| Korekta lokalizacji | dialog pozostaje stabilny | ponowna obsługa akcji | ustawienia aplikacji | TODO |

Dodatkowe scenariusze:

| Scenariusz | Oczekiwany wynik | Wynik |
| --- | --- | --- |
| Allow approximate | funkcje lokalizacji działają bez wymagania precise | TODO |
| Allow precise | mapa, Start, Lista i formularz używają dokładnej lokalizacji | TODO |
| Uprawnienie nadane, GPS wyłączony | aplikacja proponuje włączenie usługi lokalizacji | TODO |
| Powrót z ustawień po nadaniu zgody | ekran odświeża stan i pozwala wykonać akcję | TODO |
| Brak lokalizacji | główne ekrany działają w trybie ograniczonym | TODO |

## Kamera

| Scenariusz | Oczekiwany wynik | Wynik |
| --- | --- | --- |
| Allow | aparat otwiera się, a zdjęcie wraca do formularza | TODO |
| Deny | brak crasha, Photo Picker nadal działa | TODO |
| Kolejna odmowa | akcja nie staje się martwa | TODO |
| Trwała odmowa | aplikacja otwiera ustawienia aplikacji | TODO |
| Powrót z ustawień | po nadaniu zgody aparat działa | TODO |

Sprawdź wszystkie miejsca używające aparatu: dodawanie miejsca, zdjęcie miejsca, opinia i avatar, jeżeli dany ekran udostępnia kamerę.

## Photo Picker

| Obszar | Oczekiwany wynik | Wynik |
| --- | --- | --- |
| Avatar | systemowy picker bez szerokiej zgody do galerii | TODO |
| Zdjęcia miejsca | wybór jednego lub wielu zdjęć bez `READ_MEDIA_IMAGES` | TODO |
| Zdjęcia opinii | wybór zdjęć bez `READ_MEDIA_IMAGES` | TODO |
| Szczegóły miejsca | wybór zdjęcia bez szerokiej zgody | TODO |
| Brak zgody na kamerę | wybór zdjęć nadal działa | TODO |

## Powiadomienia

| Scenariusz | Oczekiwany wynik | Wynik |
| --- | --- | --- |
| Allow na Androidzie 13+ | powiadomienia mogą być dostarczane | TODO |
| Deny na Androidzie 13+ | aplikacja działa bez powiadomień | TODO |
| Ponowne uruchomienie | brak niepotrzebnej pętli dialogów | TODO |

## Kontrole statyczne

Manifest powinien deklarować:

- `INTERNET`,
- `ACCESS_NETWORK_STATE`,
- `ACCESS_FINE_LOCATION`,
- `ACCESS_COARSE_LOCATION`,
- `CAMERA`,
- `POST_NOTIFICATIONS`.

Manifest nie powinien deklarować:

- `ACCESS_BACKGROUND_LOCATION`,
- `READ_MEDIA_IMAGES`,
- `READ_EXTERNAL_STORAGE`,
- `QUERY_ALL_PACKAGES`.

Flow zdjęć powinien używać `PickVisualMedia` albo `PickMultipleVisualMedia`, a kamera osobnego runtime permission `CAMERA`.

## Szablon wyniku

```markdown
## Manual gate result

- Data:
- Tester:
- Commit / build:
- Urządzenie:
- Android:
- Wynik: PASS / FAIL / BLOCKED

### Co sprawdzono
- Lokalizacja: PASS / FAIL / BLOCKED
- Kamera: PASS / FAIL / BLOCKED
- Powiadomienia: PASS / FAIL / BLOCKED
- Photo Picker: PASS / FAIL / BLOCKED
- Powrót z ustawień aplikacji: PASS / FAIL / BLOCKED

### Dowody
- screenshot / nagranie / log / link:

### Follow-up issue
- brak / #...
```

Nie zamykaj #274 ani #303 z wynikiem `FAIL` lub `BLOCKED`, dopóki każdy problem nie ma osobnego follow-up issue.
