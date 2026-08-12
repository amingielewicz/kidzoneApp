# Google Play Data Safety — robocze odpowiedzi

Powiązane issue: #182, #210, #213, #216, #269, #271, #272, #274, #275, #303, #356

Milestone: `v1.0.0`

Ostatnia aktualizacja: 2026-08-06

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

Aplikacja obsługuje konta, treści użytkownika (miejsca, opinie, zdjęcia), lokalizację podczas używania aplikacji, diagnostykę i analitykę.

### Czy dane są szyfrowane podczas przesyłania?

```text
Tak.
```

Komunikacja z Firebase, Google APIs i Google Play Services odbywa się przez HTTPS/TLS. Finalny build nie używa endpointów cleartext (`usesCleartextTraffic=false`).

### Czy użytkownik może zażądać usunięcia danych?

```text
Tak.
```

Ścieżki:

- aplikacja: `Profil → Konto i bezpieczeństwo → Usuń konto`,
- publiczny URL: `/account-deletion`.

### Czy identyfikator użytkownika jest przesyłany do Analytics?

```text
Nie.
```

Aplikacja nie wywołuje `setUserId` w Firebase Analytics. Używane są wyłącznie anonimowe identyfikatory instancji aplikacji generowane przez Google.

## Kategorie danych

### Dane osobowe

| Dane | Zbierane | Wymagane | Cel | Widoczność |
| --- | --- | --- | --- | --- |
| adres e-mail | tak | wymagany dla konta e-mail | logowanie, konto, bezpieczeństwo | prywatna |
| nazwa użytkownika | tak | wymagana dla profilu | profil, autor treści, ranking | publiczna |
| imię i nazwisko | opcjonalnie | nie | profil i obsługa konta | prywatna |
| avatar | opcjonalnie | nie | profil użytkownika | publiczna |

### Zdjęcia

| Pole | Odpowiedź |
| --- | --- |
| Czy zbierane? | Tak |
| Czy wymagane? | Nie |
| Cel | funkcjonalność aplikacji i treści użytkownika |
| Widoczność | publiczna dla zdjęć miejsc/opinii; publiczna dla avatara |

Stan techniczny:

- aplikacja używa `PickVisualMedia` i `PickMultipleVisualMedia` (Photo Picker),
- manifest nie deklaruje `READ_MEDIA_IMAGES` ani `READ_EXTERNAL_STORAGE`,
- kamera wymaga uprawnienia `CAMERA`.

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

### Treści użytkownika

Zbierane są:

- miejsca i ich opisy,
- opinie i oceny,
- zdjęcia,
- zgłoszenia naruszeń (anonimizowane po stronie serwera),
- propozycje zmian danych miejsc.

### Diagnostyka i wydajność

| Kategoria Google Play | Źródło | Cel |
| --- | --- | --- |
| Crash logs | Crashlytics | stabilność i analiza awarii |
| Diagnostics | Crashlytics, Performance | diagnostyka i bezpieczeństwo |
| Performance data | Firebase Performance | poprawa wydajności |
| App activity | Analytics | analiza użycia funkcji |
| Device or other IDs | Firebase, FCM, Google Play Services | działanie usług, bezpieczeństwo, powiadomienia |

Aplikacja używa `CrashlyticsTree`, który automatycznie usuwa (redaguje) adresy e-mail i tokeny z treści logów wysyłanych do chmury.

## Kontrola przed zapisaniem formularza

- [x] finalny manifest nie zawiera `ACCESS_BACKGROUND_LOCATION`,
- [x] finalny manifest nie zawiera `READ_MEDIA_IMAGES` ani `READ_EXTERNAL_STORAGE`,
- [x] Photo Picker działa dla avatara, miejsc i opinii,
- [x] lista aktywnych SDK odpowiada `gradle/libs.versions.toml`,
- [x] polityka prywatności opisuje wszystkie aktywne usługi,
- [x] logi Logcat nie zawierają współrzędnych ani danych osobowych (zweryfikowano #356),
- [x] Cloud Rules (Firestore/Storage) zostały utwardzone (hasOnly, max length, isValidFileName) (#356),
- [x] mechanizmy blokowania użytkowników (ban) i akceptacji Regulaminu (TOS) są wdrożone technicznie (#373).

## Polityka UGC (User Generated Content)

Zgodnie z wymaganiami Google Play, aplikacja wdraża:
- **Akceptację Regulaminu**: Każdy użytkownik musi zaakceptować TOS przy rejestracji (`tosAcceptedAtMillis`),
- **Blokowanie użytkowników**: System umożliwia administratorowi nałożenie blokady czasowej lub permanentnej (`bannedUntilMillis`), co technicznie uniemożliwia dodawanie jakichkolwiek treści (walidacja po stronie aplikacji i reguł Firestore),
- **Zgłaszanie treści**: Użytkownicy mogą zgłaszać miejsca, opinie i zdjęcia (Issue #347).
