# v1.0.0 manual release gates

Powiązane issue: #269, #210, #272, #274, #275, #303

Ostatnia aktualizacja: 2026-07-13

## Cel

Dokument zbiera ręczne bramki wymagane przed wydaniem `v1.0.0`. Część kontroli wymaga aplikacji na urządzeniu oraz dostępu do Firebase Hosting, Firebase Console i Google Play Console.

## Kolejność

1. Wdróż Firebase Hosting i sprawdź publiczne adresy.
2. Wykonaj test usuwania konta.
3. Wykonaj test runtime permissions.
4. Uzupełnij Google Play Data Safety.
5. Wykonaj finalny przegląd prawny i produktowy.
6. Zamknij parent release blocker.

## Format dowodu

```markdown
## Manual gate result

- Data:
- Tester:
- Commit / build:
- Urządzenie:
- Android:
- Wynik: PASS / FAIL / BLOCKED

### Co sprawdzono
- ...

### Dowody
- screenshot / nagranie / log / link:

### Follow-up issue
- brak / #...
```

Nie zamykaj bramki z wynikiem `FAIL` albo `BLOCKED` bez follow-up issue.

## Gate 1: publiczne adresy

Powiązane issue: #269, #275

Sprawdź bez logowania, najlepiej w trybie incognito:

| URL | Oczekiwany wynik |
| --- | --- |
| `https://playground-705e7162.web.app/privacy-policy` | polityka prywatności ładuje się publicznie |
| `https://playground-705e7162.web.app/terms-of-service` | regulamin ładuje się publicznie |
| `https://playground-705e7162.web.app/account-deletion` | instrukcja usuwania konta ładuje się publicznie |

PASS:

- HTTPS działa,
- strony nie wymagają logowania,
- kontakt jest widoczny,
- linki między dokumentami działają,
- daty i treść odpowiadają wersji release.

## Gate 2: usuwanie konta

Powiązane issue: #210

Dokument szczegółowy: `docs/legal/account-deletion-test-checklist.md`.

Minimalny test:

1. Utwórz konto testowe.
2. Dodaj profil, miejsce, opinię i zdjęcie.
3. Zapisz UID i adres e-mail.
4. Usuń konto przez `Profil → Konto i bezpieczeństwo → Usuń konto`.
5. Sprawdź Authentication, Firestore i Storage.
6. Spróbuj zalogować się ponownie.
7. Sprawdź widok treści użytkownika z innego konta.
8. Uruchom aplikację ponownie i sprawdź lokalny cache.

PASS:

- stare konto nie pozwala się zalogować,
- dane prywatne są usunięte albo zanonimizowane,
- publiczne treści nie ujawniają danych identyfikujących,
- aplikacja nie crashuje,
- publiczna instrukcja usuwania konta odpowiada zachowaniu aplikacji.

## Gate 3: Android permissions

Powiązane issue: #274, #303

Dokumenty:

- `docs/legal/android-permissions-play-compliance.md`,
- `docs/qa/android-permissions-device-matrix.md`.

Sprawdź na Androidzie 13, 14 i 15, a na Androidzie 16, jeśli jest dostępny.

### Lokalizacja

- pierwsza odmowa,
- kolejna odmowa,
- trwała odmowa,
- przekierowanie do ustawień aplikacji,
- powrót po nadaniu zgody,
- approximate i precise,
- GPS wyłączony przy nadanym uprawnieniu.

Punkty wejścia:

- Start: „Włącz lokalizację”,
- Lista: sortowanie „Od najbliższych”,
- Mapa: „Moja lokalizacja”,
- dodawanie i korekta lokalizacji miejsca.

### Kamera

- allow,
- deny,
- trwała odmowa,
- przekierowanie do ustawień,
- Photo Picker działa bez kamery.

### Zdjęcia

- avatar,
- zdjęcia miejsca,
- zdjęcia opinii,
- zdjęcie ze szczegółów miejsca,
- brak szerokiego dialogu dostępu do galerii.

### Powiadomienia

- allow i deny na Androidzie 13+,
- aplikacja działa poprawnie bez zgody.

PASS:

- brak `ACCESS_BACKGROUND_LOCATION`,
- brak `READ_MEDIA_IMAGES` i `READ_EXTERNAL_STORAGE`,
- Photo Picker wybiera konkretne pliki,
- po trwałej odmowie akcje prowadzą do ustawień aplikacji,
- żaden przycisk nie staje się martwy po kolejnych odmowach,
- aplikacja działa bez lokalizacji, kamery i powiadomień.

## Gate 4: Google Play Data Safety

Powiązane issue: #272

Dokument roboczy: `docs/legal/google-play-data-safety-draft.md`.

Potwierdź w Google Play Console:

- aplikacja zbiera dane użytkownika,
- dane są szyfrowane podczas transmisji,
- użytkownik może zażądać usunięcia danych,
- lokalizacja jest używana tylko podczas korzystania z aplikacji,
- brak background location,
- zdjęcia są wybierane przez użytkownika i są opcjonalne,
- diagnostyka obejmuje używane usługi Firebase,
- deklaracje odpowiadają manifestowi, polityce prywatności i realnej konfiguracji SDK.

PASS:

- formularz jest zapisany,
- odpowiedzi odpowiadają aplikacji,
- dowód finalnych odpowiedzi został dodany do #272.

## Gate 5: finalny przegląd prawny i produktowy

Powiązane issue: #275

Sprawdź:

- publiczne adresy i daty dokumentów,
- dane operatora oraz kontakt,
- regulamin i politykę prywatności,
- opis usuwania konta,
- listę aktywnych usług Firebase,
- zgodność Data Safety,
- linki dostępne z aplikacji,
- ostrzeżenia dotyczące publikowania zdjęć dzieci i osób trzecich.

PASS:

- dokumenty są publiczne i kompletne,
- treść odpowiada aplikacji,
- nie ma rozjazdu między manifestem, Google Play Console i polityką prywatności.

## Gate 6: parent release blocker

Powiązane issue: #269

Zamknij dopiero, gdy:

- #210 ma PASS,
- #272 ma PASS,
- #274 i #303 mają PASS,
- #275 ma PASS,
- wszystkie poprawki wykryte podczas testów są zmergowane.

```markdown
## Legal & Google Play release blocker result

- Publiczne URL-e: PASS / FAIL / BLOCKED
- Account deletion QA (#210): PASS / FAIL / BLOCKED
- Runtime permissions QA (#274, #303): PASS / FAIL / BLOCKED
- Google Play Data Safety (#272): PASS / FAIL / BLOCKED
- Final legal review (#275): PASS / FAIL / BLOCKED
- Wynik ogólny: PASS / FAIL / BLOCKED
```
