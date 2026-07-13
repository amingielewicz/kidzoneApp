# Play Store Release

Ostatnia aktualizacja: 2026-07-13

## Cel

Dokument opisuje minimalny proces publikacji kidZone w Google Play, od przygotowania builda po kontrolę staged rollout.

## Dokumenty źródłowe

- `docs/release/RELEASE_PROCESS.md`
- `docs/release/GO_NO_GO_CHECKLIST.md`
- `docs/qa/google-play-security-checklist.md`
- `docs/qa/manual-release-test-plan.md`
- `docs/legal/google-play-data-safety-draft.md`
- `docs/legal/v1-release-manual-gates.md`

## Przed publikacją

- [ ] `versionCode` zwiększony,
- [ ] `versionName` ustawiony,
- [ ] signed AAB wygenerowany,
- [ ] release notes przygotowane,
- [ ] GO / NO-GO wykonane,
- [ ] Crashlytics i App Check zweryfikowane,
- [ ] Firestore Rules i Storage Rules wdrożone,
- [ ] brak sekretów i danych wrażliwych w logach,
- [ ] Data Safety zapisane w Play Console,
- [ ] publiczne URL-e działają,
- [ ] usuwanie konta ma wynik PASS,
- [ ] runtime permissions mają wynik PASS.

## Uprawnienia i prywatność

Finalny manifest powinien deklarować wyłącznie wymagany zakres:

- `ACCESS_FINE_LOCATION` i `ACCESS_COARSE_LOCATION` jako opcjonalne runtime permissions,
- `CAMERA` jako opcjonalne runtime permission,
- `POST_NOTIFICATIONS` na Androidzie 13+.

Finalny manifest nie powinien zawierać:

- `ACCESS_BACKGROUND_LOCATION`,
- `READ_MEDIA_IMAGES`,
- `READ_EXTERNAL_STORAGE`,
- `QUERY_ALL_PACKAGES`.

Zdjęcia z galerii są wybierane przez Android Photo Picker. Po trwałej odmowie lokalizacji albo kamery aplikacja powinna kierować do ustawień aplikacji.

## Internal testing

Sprawdź:

- [ ] czystą instalację,
- [ ] aktualizację z poprzedniej wersji,
- [ ] logowanie i rejestrację,
- [ ] Start, Mapę, Listę i szczegóły miejsca,
- [ ] ranking i profil,
- [ ] dodawanie miejsca, opinii oraz zdjęć,
- [ ] brak internetu,
- [ ] działanie bez lokalizacji,
- [ ] działanie bez kamery,
- [ ] działanie bez powiadomień,
- [ ] pierwszą i trwałą odmowę uprawnień,
- [ ] powrót z ustawień aplikacji.

## Closed testing

Wymagane:

- [ ] test na kilku wersjach Androida,
- [ ] test na kilku producentach urządzeń,
- [ ] test na słabszym urządzeniu,
- [ ] test użytkownika nietechnicznego,
- [ ] kontrola Crashlytics i Performance,
- [ ] kontrola kosztów Firebase i Maps,
- [ ] potwierdzenie, że Photo Picker nie prosi o szeroki dostęp do galerii.

## Store listing

Przed publicznym wydaniem sprawdź:

- [ ] nazwę aplikacji,
- [ ] krótki i pełny opis,
- [ ] ikonę i feature graphic,
- [ ] screenshoty,
- [ ] kategorię Parenting,
- [ ] grupę docelową: rodzice i opiekunowie,
- [ ] content rating,
- [ ] informację o reklamach i zakupach,
- [ ] Privacy Policy URL,
- [ ] Account deletion URL,
- [ ] Data Safety,
- [ ] deklaracje uprawnień.

## Production rollout

Rekomendowany staged rollout:

```text
5% → 20% → 50% → 100%
```

Przed zwiększeniem rollout sprawdź:

- [ ] Android vitals,
- [ ] crash rate i ANR,
- [ ] problemy z logowaniem,
- [ ] działanie mapy i listy,
- [ ] błędy usuwania konta,
- [ ] problemy ze zdjęciami i uprawnieniami,
- [ ] nietypowy wzrost kosztów,
- [ ] zgłoszenia P0/P1.

## Po publikacji

Sprawdź:

- [ ] Crashlytics po pierwszych wdrożeniach,
- [ ] Android vitals po 24 godzinach,
- [ ] Firebase usage,
- [ ] Maps usage,
- [ ] zgłoszenia użytkowników,
- [ ] opinie w sklepie,
- [ ] poprawność publicznych dokumentów.

## Halt rollout

Zatrzymaj rollout, gdy:

- crash rate lub ANR wyraźnie rośnie,
- logowanie albo usuwanie konta nie działa,
- mapa lub lista jest niedostępna,
- występuje problem privacy/security,
- uprawnienia powodują martwe akcje albo pętle,
- koszty Firebase lub Maps rosną nietypowo,
- pojawiają się powtarzalne zgłoszenia P0/P1.

## Wynik

```text
Release:
Commit / tag:
AAB:
Track:
Data Safety: PASS / FAIL / BLOCKED
Account deletion: PASS / FAIL / BLOCKED
Runtime permissions: PASS / FAIL / BLOCKED
Security checklist: PASS / FAIL / BLOCKED
GO / NO-GO:
Dowody:
```
