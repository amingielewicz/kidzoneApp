# v1.0.0 manual release gates

Powiazane issue: #269, #210, #272, #274, #275

Ostatnia aktualizacja: 2026-06-29

## Cel

Ten dokument zbiera ostatnie reczne bramki przed zamknieciem milestone `v1.0.0`.
Kod i dokumenty w repo sa przygotowane, ale ponizsze punkty wymagaja realnego
sprawdzenia poza samym repozytorium: aplikacja na urzadzeniu, Firebase Console,
Firebase Hosting i Google Play Console.

## Kolejnosc zamykania

Rekomendowana kolejnosc:

1. Wdrozyc Firebase Hosting i sprawdzic publiczne URL-e.
2. Wykonac account deletion QA z #210.
3. Wykonac Android permissions QA z #274.
4. Przepisac Google Play Data Safety z #272.
5. Wykonac final legal review z #275.
6. Zamknac parent release blocker #269.

## Evidence format

Do kazdego issue dopisz komentarz w tym formacie:

```markdown
## Manual gate result

- Data:
- Tester:
- Commit / build:
- Urzadzenie:
- Wynik: PASS / FAIL / BLOCKED

### Co sprawdzono
- ...

### Dowody
- screenshot/log/link:

### Follow-up issue
- brak / #...
```

Nie zamykac issue z wynikiem `FAIL` albo `BLOCKED`.

## Gate 1: publiczne URL-e

Powiazane issue: #269, #275

Po deployu Firebase Hosting sprawdzic bez logowania, w trybie prywatnym/incognito:

| URL | Oczekiwany wynik |
| --- | --- |
| `https://playground-705e7162.web.app/privacy-policy` | Polityka prywatnosci laduje sie publicznie. |
| `https://playground-705e7162.web.app/privacy-policy.html` | Polityka prywatnosci laduje sie publicznie. |
| `https://playground-705e7162.web.app/terms-of-service` | Regulamin laduje sie publicznie. |
| `https://playground-705e7162.web.app/terms-of-service.html` | Regulamin laduje sie publicznie. |
| `https://playground-705e7162.web.app/account-deletion` | Instrukcja usuwania konta laduje sie publicznie. |
| `https://playground-705e7162.web.app/account-deletion.html` | Instrukcja usuwania konta laduje sie publicznie. |

PASS:

- wszystkie URL-e dzialaja po HTTPS,
- strony nie wymagaja logowania,
- kontakt e-mail jest widoczny,
- strony linkuja miedzy soba,
- daty dokumentow sa aktualne dla release.

## Gate 2: account deletion QA

Powiazane issue: #210

Dokument szczegolowy:

```text
docs/legal/account-deletion-test-checklist.md
```

Minimalny test:

1. Utworz konto testowe.
2. Ustaw nazwe uzytkownika, opcjonalne dane profilu i avatar.
3. Dodaj miejsce.
4. Dodaj opinie.
5. Dodaj zdjecie do miejsca albo opinii.
6. Zapisz UID i e-mail konta.
7. Usun konto w aplikacji: `Profil -> Konto i bezpieczenstwo -> Usun konto`.
8. Sprawdz Firebase Authentication.
9. Sprawdz Firestore: `users/{uid}` oraz `users/{uid}/private/*`.
10. Sprawdz Firebase Storage dla avatara i zdjec.
11. Sprawdz ponowne logowanie starym kontem.
12. Sprawdz, czy aplikacja nie pokazuje prywatnych danych w cache po restarcie.

PASS:

- konto nie pozwala sie zalogowac po usunieciu,
- prywatne dane sa usuniete albo zanonimizowane,
- tokeny FCM nie zostaja publicznie dostepne,
- publiczne tresci nie pokazuja e-maila, imienia ani nazwiska,
- aplikacja nie crashuje po usunieciu konta.

## Gate 3: Android permissions QA

Powiazane issue: #274

Dokument szczegolowy:

```text
docs/legal/android-permissions-play-compliance.md
docs/qa/android-permissions-device-matrix.md
```

Sprawdzic na Androidzie 13+ lub 14+:

| Obszar | Scenariusze |
| --- | --- |
| Lokalizacja | deny, approximate, precise, ponowna proba po odmowie. |
| Kamera | allow, deny, dzialanie bez kamery przy wyborze z galerii. |
| Powiadomienia | allow, deny, ustawienia po odmowie. |
| Zdjecia | avatar, zdjecia miejsca, zdjecia opinii, szczegoly miejsca. |

PASS:

- aplikacja nie deklaruje `ACCESS_BACKGROUND_LOCATION`,
- aplikacja nie deklaruje `READ_MEDIA_IMAGES`,
- Photo Picker nie pokazuje systemowego dialogu o szerokim dostepie do galerii,
- funkcje zdjec dzialaja przez wybor konkretnych plikow,
- aplikacja dziala bez lokalizacji i bez powiadomien.

## Gate 4: Google Play Data Safety

Powiazane issue: #272

Dokument z odpowiedziami roboczymi:

```text
docs/legal/google-play-data-safety-draft.md
```

W Google Play Console wpisac i potwierdzic:

- aplikacja zbiera dane uzytkownika: tak,
- dane sa szyfrowane w transmisji: tak,
- uzytkownik moze zadac usuniecia danych: tak,
- Privacy Policy URL: `https://playground-705e7162.web.app/privacy-policy`,
- Account deletion URL: `https://playground-705e7162.web.app/account-deletion`,
- lokalizacja: tylko podczas uzywania aplikacji,
- brak background location,
- zdjecia: wybierane przez uzytkownika, opcjonalne,
- crash logs / diagnostics / performance: Firebase Crashlytics i Performance,
- analytics / app activity: Firebase Analytics,
- device or other IDs: Firebase/Google Play Services/FCM.

PASS:

- formularz zapisany w Google Play Console,
- odpowiedzi zgadzaja sie z `public/privacy-policy.html`,
- odpowiedzi zgadzaja sie z aktualnym manifestem,
- screenshot albo notatka z finalnych odpowiedzi jest dodana do #272.

## Gate 5: final legal release review

Powiazane issue: #275

Sprawdzic:

- publiczne URL-e,
- daty dokumentow,
- kontakt e-mail,
- opis usuwania konta,
- zgodnosc Privacy Policy z Firebase SDK,
- zgodnosc Terms z funkcjami aplikacji,
- czy aplikacja w Profilu pokazuje: Regulamin, Polityka prywatnosci, Kontakt, Usun konto,
- czy Google Play Data Safety zostala przepisana i zapisana.

PASS:

- dokumenty sa kompletne i publiczne,
- dokumenty sa zgodne z aplikacja,
- nie ma rozjazdu miedzy Play Console, Privacy Policy i realnym manifestem,
- finalny komentarz PASS jest wpisany do #275.

## Gate 6: parent release blocker

Powiazane issue: #269

Zamknac dopiero, gdy:

- #210 ma PASS albo ma osobne follow-up issue dla brakow,
- #272 ma PASS w Google Play Console,
- #274 ma PASS na urzadzeniu i w Play Console,
- #275 ma finalny PASS,
- wszystkie PR-e powiazane z legal/release sa zmergowane.

Komentarz do #269:

```markdown
## Legal & Google Play release blocker result

- Publiczne URL-e: PASS / FAIL
- Account deletion QA (#210): PASS / FAIL / BLOCKED
- Runtime permissions QA (#274): PASS / FAIL / BLOCKED
- Google Play Data Safety (#272): PASS / FAIL / BLOCKED
- Final legal review (#275): PASS / FAIL / BLOCKED
- Wynik ogolny: PASS / FAIL / BLOCKED
```
