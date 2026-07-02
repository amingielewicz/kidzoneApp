# Android permissions and device compatibility matrix

Powiazane issue: #274, #303

Ostatnia aktualizacja: 2026-07-02

## Cel

Ten dokument jest robocza macierza manualnego QA dla runtime permissions,
Photo Pickera i kompatybilnosci urzadzen przed publikacja kidZone w Google Play.

Nie zastepuje testu na realnym urzadzeniu. Ma zapewnic, ze wynik PASS/FAIL dla
#274 i #303 jest zapisany w jednym, powtarzalnym formacie.

## Zakres builda

Przed testem zapisz:

| Pole | Wartosc |
| --- | --- |
| Commit / tag | |
| Build type | debug / release / internal |
| Firebase project | |
| MAPS_API_KEY | placeholder / produkcyjny |
| Tester | |
| Data | |

## Macierz urzadzen

Minimalna macierz:

| Android | Urzadzenie | Status | Uwagi |
| --- | --- | --- | --- |
| 13 | fizyczne lub emulator | TODO | Powiadomienia wymagaja runtime permission. |
| 14 | fizyczne lub emulator | TODO | Sprawdzic Photo Picker i denial flows. |
| 15 | fizyczne lub emulator, jesli dostepne | TODO | Smoke test zgodnosci. |

Dodatkowo warto sprawdzic jedno urzadzenie producenta z mocno zmienionym Androidem
(np. Xiaomi/MIUI), bo systemowe dialogi i ustawienia uprawnien bywaja inne.

## Checklist per urzadzenie

Skopiuj tabele dla kazdego testowanego urzadzenia.

| Obszar | Scenariusz | Oczekiwany wynik | Wynik |
| --- | --- | --- | --- |
| Lokalizacja | Deny przy pierwszym pytaniu | Aplikacja nie crashuje, mapa i lista dzialaja w trybie bez lokalizacji. | TODO |
| Lokalizacja | Allow approximate | Aplikacja pokazuje miejsca w poblizu bez wymagania precise. | TODO |
| Lokalizacja | Allow precise | Mapa, Start, Lista i AddPlace uzywaja dokladnej lokalizacji. | TODO |
| Lokalizacja | Ponowna proba po odmowie | UI pozwala ponowic prosbe albo przejsc do ustawien, bez petli dialogow. | TODO |
| Kamera | Allow | Aparat otwiera sie z AddPlace i szczegolow/opinii, zdjecie wraca do formularza. | TODO |
| Kamera | Deny | Aplikacja nie crashuje; mozna wybrac zdjecie przez Photo Picker. | TODO |
| Kamera | Deny + nie pytaj ponownie, jesli system pokazuje | UI nie blokuje formularza i nie crashuje. | TODO |
| Powiadomienia | Allow na Androidzie 13+ | Aplikacja zapisuje zgode i nie pyta ponownie bez potrzeby. | TODO |
| Powiadomienia | Deny na Androidzie 13+ | Aplikacja dziala dalej bez powiadomien. | TODO |
| Photo Picker | Avatar profilu | Otwiera sie systemowy picker bez szerokiego dostepu do galerii. | TODO |
| Photo Picker | Zdjecia miejsca | Mozna wybrac zdjecia miejsca bez `READ_MEDIA_IMAGES`. | TODO |
| Photo Picker | Zdjecia opinii | Mozna wybrac zdjecia opinii bez `READ_MEDIA_IMAGES`. | TODO |
| Photo Picker | Zdjecie w szczegolach miejsca | Mozna wybrac zdjecie bez szerokiego dostepu do galerii. | TODO |
| Bez lokalizacji | Start / Mapa / Lista / AddPlace | Ekrany pokazuja czytelny stan ograniczony, bez crasha. | TODO |
| Bez powiadomien | Uruchomienie i glowne zakladki | Aplikacja dziala normalnie, push jest opcjonalny. | TODO |
| Bez kamery | Dodawanie zdjec | Uzytkownik moze uzyc Photo Pickera. | TODO |

## Kontrole statyczne w repo

Na 2026-07-02 manifest deklaruje:

- `INTERNET`
- `ACCESS_NETWORK_STATE`
- `ACCESS_FINE_LOCATION`
- `ACCESS_COARSE_LOCATION`
- `CAMERA`
- `POST_NOTIFICATIONS`

Manifest nie deklaruje:

- `ACCESS_BACKGROUND_LOCATION`
- `READ_MEDIA_IMAGES`
- `READ_EXTERNAL_STORAGE`
- `QUERY_ALL_PACKAGES`

Flow zdjec w kodzie uzywa `ActivityResultContracts.PickVisualMedia` albo
`ActivityResultContracts.PickMultipleVisualMedia`, a kamera ma osobne
runtime permission `CAMERA`.

## Szablon komentarza do issue

Wynik dopisz do #274 i #303:

```markdown
## Manual gate result

- Data:
- Tester:
- Commit / build:
- Urzadzenie:
- Android:
- Wynik: PASS / FAIL / BLOCKED

### Co sprawdzono
- Lokalizacja: PASS / FAIL / BLOCKED
- Kamera: PASS / FAIL / BLOCKED
- Powiadomienia: PASS / FAIL / BLOCKED
- Photo Picker: PASS / FAIL / BLOCKED
- Dzialanie bez odmowionych uprawnien: PASS / FAIL / BLOCKED

### Dowody
- screenshot/log/link:

### Follow-up issue
- brak / #...
```

Nie zamykac #274 ani #303 z wynikiem `FAIL` lub `BLOCKED`, chyba ze kazdy
problem ma osobne follow-up issue.
