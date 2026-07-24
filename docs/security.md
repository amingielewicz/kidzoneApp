# Security documentation

Powiązane issue: #159, #166, #290, #291, #292, #293, #294, #298

Ostatnia aktualizacja: 2026-07-13

## Cel

Dokument opisuje podstawowe zasady bezpieczeństwa kidZone dla developmentu, testów i release. Nie zastępuje niezależnego audytu bezpieczeństwa.

## Powiązane dokumenty

- `docs/app-check.md`
- `docs/abuse-rate-limiting.md`
- `docs/firebase-security-plan.md`
- `docs/qa/google-play-security-checklist.md`
- `docs/operations/INCIDENT_RESPONSE.md`

## Zasady nadrzędne

- Nie commitujemy sekretów ani keystore.
- Nie logujemy danych osobowych, tokenów ani dokładnej lokalizacji.
- Dane prywatne nie trafiają do publicznych dokumentów Firestore.
- Reguły Firebase nie używają `allow read, write: if true` poza lokalnym emulatorem.
- Debug provider i debug konfiguracja nie mogą działać w release.
- Każda zmiana danych, reguł lub uprawnień wymaga aktualizacji testów i dokumentacji.

## Dane użytkownika

### Publiczne

- nazwa użytkownika,
- avatar, jeśli UI pokazuje go publicznie,
- miejsca, opinie, oceny i publiczne zdjęcia.

### Prywatne

- e-mail,
- imię i nazwisko,
- ustawienia konta,
- tokeny FCM,
- informacje o blokadzie,
- dane administracyjne.

Docelowe ścieżki:

```text
users/{uid}/private/profile
users/{uid}/private/messaging
```

Dostęp powinien mieć właściciel konta albo autoryzowany administrator.

## Firebase Authentication

- operacje użytkownika wymagają zalogowania,
- własność sprawdzamy przez `request.auth.uid`,
- rola admina jest weryfikowana po stronie zaufanej,
- nie ufamy UID przesłanemu przez klienta,
- usuwanie konta obsługuje `requires-recent-login`,
- anulowany reauth nie może powodować częściowego usunięcia danych.

## Firestore Security Rules

Każda kolekcja ma jawne reguły. Kontrolujemy odczyt, tworzenie, aktualizację, usuwanie, ownership, pola administracyjne, eskalację uprawnień oraz rozdzielenie danych publicznych i prywatnych.

Reguły powinny mieć testy emulatorowe dla scenariuszy pozytywnych i negatywnych.

## Storage Security Rules

- zapis tylko do dozwolonych ścieżek właścicielskich,
- brak nadpisywania cudzych plików,
- walidacja MIME i rozmiaru,
- kontrola usuwania,
- zgodność z account deletion,
- legacy paths bez otwartego zapisu.

Przykładowe ścieżki:

```text
places/{ownerUserId}/{placeId}/photos/{fileName}
reviews/{ownerUserId}/{reviewId}/photos/{fileName}
users/{ownerUserId}/avatar/{fileName}
```

## App Check

- Debug provider tylko w debug buildach.
- Release używa Play Integrity.
- Debug tokeny nie trafiają do repo ani logów.
- Enforcement włączamy po obserwacji metryk i smoke teście.
- Prawidłowe buildy nie mogą być blokowane przez błędną konfigurację.

## Sekrety i klucze

Sekretami są między innymi:

- keystore i hasła,
- service account JSON,
- tokeny GitHub,
- dane Firebase Admin,
- dane konta Google Play,
- nieograniczone klucze API.

Klucz Google Maps musi być ograniczony do właściwego package name i SHA-1/SHA-256. Należy ograniczyć listę aktywnych API oraz ustawić limity i alerty kosztowe.

## GitHub Actions

- sekrety przechowujemy w GitHub Secrets,
- nie wypisujemy sekretów w logach,
- workflow z niezaufanego PR nie dostaje sekretów,
- Gitleaks blokuje wykryte sekrety,
- uprawnienia workflow ustawiamy według zasady najmniejszych uprawnień,
- po podejrzeniu wycieku sekret natychmiast rotujemy.

## Keystore

- nie trafia do repo,
- ma zaszyfrowaną kopię zapasową,
- hasła są w menedżerze haseł,
- dostęp jest ograniczony i udokumentowany,
- tag release wskazuje commit podpisanego builda.

## Logowanie i Crashlytics

Nie logujemy:

- haseł,
- tokenów sesji i FCM,
- pełnych e-maili,
- prywatnych danych profilu,
- dokładnych współrzędnych użytkownika,
- treści formularzy zawierających dane osobowe.

Crashlytics:

- nie dostaje surowych danych wyjątków pochodzących z treści użytkownika,
- custom keys i breadcrumbs nie zawierają PII,
- identyfikator użytkownika jest wyłączony albo pseudonimizowany zgodnie z decyzją prywatności,
- zakres zbierania odpowiada Data Safety.

## Analytics i Performance

- event parameters nie zawierają e-maili, nazwisk, pełnych adresów ani dokładnej lokalizacji,
- eventy mają neutralne nazwy,
- user properties są ograniczone do minimum,
- custom traces nie zawierają danych użytkownika,
- aktywne usługi są opisane w polityce prywatności i Data Safety.

## FCM

- tokeny są przechowywane prywatnie,
- nie są logowane ani publicznie czytelne,
- są usuwane lub unieważniane przy logout i delete account,
- odmowa `POST_NOTIFICATIONS` nie blokuje aplikacji.

## Android permissions

Aplikacja używa tylko foreground location, opcjonalnej kamery i opcjonalnych powiadomień.

Nie deklaruje:

- `ACCESS_BACKGROUND_LOCATION`,
- `READ_MEDIA_IMAGES`,
- `READ_EXTERNAL_STORAGE`,
- `QUERY_ALL_PACKAGES`.

Zdjęcia są wybierane przez Android Photo Picker. Po trwałej odmowie lokalizacji lub kamery aplikacja otwiera ustawienia aplikacji. Żaden punkt wejścia nie może stać się martwy po kolejnych odmowach.

## Zależności

Przed release:

```powershell
.\gradlew detekt
.\gradlew lint
.\gradlew testDebugUnitTest
```

Dodatkowo przeglądamy aktualizacje i podatności zależności. Aktualizacje krytycznych bibliotek wykonujemy pojedynczo lub w kontrolowanych grupach z regresją.

## Backup i odzyskiwanie

Backup obejmuje keystore, konfigurację i listę nazw sekretów, dokumentację release, politykę prywatności, regulamin i procedury operacyjne.

Sekretów nie przechowujemy w plain text.

## Reakcja na incydent

1. Zatrzymaj wdrożenia lub rollout.
2. Określ zakres i wpływ.
3. Obróć ujawnione sekrety.
4. Popraw i wdróż reguły lub konfigurację.
5. Sprawdź logi Firebase, GCP i GitHub.
6. Oceń obowiązek poinformowania użytkowników.
7. Utwórz raport incydentu i follow-up issues.
8. Dodaj test lub kontrolę zapobiegającą powtórce.

## Checklista dla PR

Przy zmianie danych, Firebase, uprawnień lub logowania sprawdź:

- [ ] wpływ na dane użytkownika,
- [ ] potrzebę aktualizacji Rules,
- [ ] wpływ na Data Safety i politykę prywatności,
- [ ] ryzyko logowania PII,
- [ ] zachowanie dla odmów uprawnień,
- [ ] testy negatywne,
- [ ] rollback i obserwowalność.
