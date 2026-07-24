# Google Play Data Safety — robocze odpowiedzi

Powiązane issue: #182, #210, #213, #216, #269, #271, #272, #274, #275, #303

Milestone: `v1.0.0`

Ostatnia aktualizacja: 2026-07-13

## Cel

Dokument zawiera robocze odpowiedzi do formularza Google Play Data Safety. Ostateczne deklaracje muszą zostać porównane z finalnym buildem, manifestem, aktywnymi usługami Firebase oraz polityką prywatności.

## Status

| Obszar | Status | Następny krok |
| --- | --- | --- |
| Polityka prywatności | gotowa w repo | sprawdzić publiczny URL |
| Regulamin | gotowy w repo | sprawdzić publiczny URL |
| Usuwanie konta | gotowe w repo | wykonać test manualny #210 |
| Runtime permissions | gotowe technicznie | wykonać #274 i #303 |
| Data Safety w Play Console | otwarte | przepisać i zapisać odpowiedzi #272 |
| Final legal review | otwarte | wykonać #275 |

## Aktywne usługi

| SDK / usługa | Status | Dane / cel |
| --- | --- | --- |
| Firebase Authentication | używane | konto, e-mail, uwierzytelnianie |
| Cloud Firestore | używane | profile, miejsca, opinie, zgłoszenia |
| Firebase Storage | używane | zdjęcia miejsc, opinii i avatarów |
| Firebase Crashlytics | używane w release | crash logs i diagnostyka |
| Firebase Analytics | używane | aktywność w aplikacji i zdarzenia |
| Firebase Performance | używane w release | dane wydajnościowe i diagnostyka |
| Firebase Cloud Messaging | używane | tokeny FCM i powiadomienia |
| Firebase App Check | używane | bezpieczeństwo i integralność aplikacji |
| Firebase Remote Config | używane | konfiguracja, eksperymenty i maintenance mode |
| Google Maps Platform | używane | mapa i funkcje lokalizacyjne |

## Odpowiedzi główne

### Czy aplikacja zbiera dane użytkownika?

```text
Tak.
```

Aplikacja obsługuje konta, treści użytkownika, zdjęcia, lokalizację podczas używania aplikacji, diagnostykę i analitykę.

### Czy dane są szyfrowane podczas przesyłania?

```text
Tak.
```

Komunikacja z Firebase, Google APIs i Google Play Services odbywa się przez HTTPS/TLS. Finalny build nie może używać endpointów cleartext.

### Czy użytkownik może zażądać usunięcia danych?

```text
Tak.
```

Ścieżki:

- aplikacja: `Profil → Konto i bezpieczeństwo → Usuń konto`,
- publiczny URL: `/account-deletion`.

Odpowiedź można zatwierdzić dopiero po przejściu testu #210.

## Kategorie danych

### Dane osobowe

| Dane | Zbierane | Wymagane | Cel | Widoczność |
| --- | --- | --- | --- | --- |
| adres e-mail | tak | wymagany dla konta e-mail | logowanie, konto, bezpieczeństwo | prywatna |
| nazwa użytkownika | tak | wymagana dla profilu | profil, autor treści, ranking | publiczna |
| imię i nazwisko | opcjonalnie | nie | profil i obsługa konta | prywatna |
| avatar | opcjonalnie | nie | profil użytkownika | zależnie od UI |

### Zdjęcia

| Pole | Odpowiedź |
| --- | --- |
| Czy zbierane? | Tak |
| Czy wymagane? | Nie |
| Cel | funkcjonalność aplikacji i treści użytkownika |
| Widoczność | publiczna dla zdjęć miejsc/opinii; zależna od UI dla avatara |

Stan techniczny:

- aplikacja używa `PickVisualMedia` i `PickMultipleVisualMedia`,
- manifest nie deklaruje `READ_MEDIA_IMAGES`,
- manifest nie deklaruje `READ_EXTERNAL_STORAGE`,
- kamera ma osobne opcjonalne uprawnienie `CAMERA`,
- użytkownik wybiera konkretne pliki przez Android Photo Picker.

Do formularza nie deklarujemy szerokiego dostępu do galerii jako wymaganej funkcji aplikacji.

### Lokalizacja

| Pole | Odpowiedź |
| --- | --- |
| Czy zbierana? | Tak |
| Czy wymagana? | Nie |
| Zakres | tylko podczas używania aplikacji |
| Cel | mapa, miejsca w pobliżu, sortowanie po odległości, dodawanie miejsca |
| Udostępnianie publiczne | Nie |

Deklaracja:

```text
Lokalizacja tylko podczas używania aplikacji.
Brak lokalizacji w tle.
Brak historii lokalizacji użytkownika.
```

Manifest nie deklaruje `ACCESS_BACKGROUND_LOCATION`.

### Treści użytkownika

Zbierane są:

- miejsca i ich opisy,
- opinie i oceny,
- zdjęcia,
- zgłoszenia naruszeń,
- propozycje zmian danych miejsc.

Cele:

- funkcjonalność aplikacji,
- społeczność,
- moderacja,
- bezpieczeństwo.

### Diagnostyka i wydajność

| Kategoria Google Play | Źródło | Cel |
| --- | --- | --- |
| Crash logs | Crashlytics | stabilność i analiza awarii |
| Diagnostics | Crashlytics, Performance | diagnostyka i bezpieczeństwo |
| Performance data | Firebase Performance | poprawa wydajności |
| App activity | Analytics | analiza użycia funkcji |
| Device or other IDs | Firebase, FCM, Google Play Services | działanie usług, bezpieczeństwo, powiadomienia |

Raportowanie błędów nie powinno przekazywać surowych wyjątków ani danych użytkownika jako custom keys lub breadcrumbs.

### Powiadomienia

FCM wykorzystuje token rejestracyjny urządzenia przechowywany w prywatnej części profilu. Powiadomienia są opcjonalne, a odmowa `POST_NOTIFICATIONS` nie blokuje aplikacji.

## Zbieranie a udostępnianie

Google Play rozróżnia zbieranie danych od ich udostępniania. Dane przetwarzane przez Firebase i Google jako dostawców usług należy ocenić według aktualnej definicji formularza w Play Console.

Nie zaznaczaj automatycznie „brak udostępniania” bez porównania z definicją Google Play i warunkami używanych SDK.

## Opcjonalność danych

| Dane | Opcjonalność |
| --- | --- |
| konto i e-mail | wymagane dla funkcji konta |
| nazwa użytkownika | wymagana dla profilu społecznościowego |
| imię i nazwisko | opcjonalne |
| lokalizacja | opcjonalna |
| kamera | opcjonalna |
| zdjęcia | opcjonalne |
| powiadomienia | opcjonalne |
| treści użytkownika | opcjonalne |
| diagnostyka i analityka | zależne od konfiguracji release |

## Kontrola przed zapisaniem formularza

- [ ] finalny manifest nie zawiera `ACCESS_BACKGROUND_LOCATION`,
- [ ] finalny manifest nie zawiera `READ_MEDIA_IMAGES` ani `READ_EXTERNAL_STORAGE`,
- [ ] Photo Picker działa dla avatara, miejsc i opinii,
- [ ] lista aktywnych SDK odpowiada `gradle/libs.versions.toml` i buildowi release,
- [ ] polityka prywatności opisuje wszystkie aktywne usługi,
- [ ] usuwanie konta ma wynik PASS,
- [ ] Crashlytics i Analytics nie zawierają danych osobowych w custom parametrach,
- [ ] odpowiedzi są zapisane w Google Play Console,
- [ ] screenshot lub eksport odpowiedzi jest dodany do #272.

## Ostateczny wynik

```markdown
## Google Play Data Safety result

- Data:
- Build / commit:
- Formularz zapisany: TAK / NIE
- Privacy Policy URL: PASS / FAIL
- Account deletion URL: PASS / FAIL
- Account deletion QA: PASS / FAIL / BLOCKED
- Manifest zgodny: PASS / FAIL
- SDK zgodne z deklaracją: PASS / FAIL
- Wynik: PASS / FAIL / BLOCKED
- Dowody:
```

Dokument jest materiałem roboczym. Źródłem prawdy przy publikacji jest finalny build i formularz zapisany w Google Play Console.
