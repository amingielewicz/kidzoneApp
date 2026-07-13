# GO / NO-GO Checklist

Ostatnia aktualizacja: 2026-07-13

## Cel

Finalna decyzja jakościowa przed publikacją kidZone w Google Play.

Statusy:

- `OK` — sprawdzone,
- `Do poprawy` — blokuje lub wymaga jawnej decyzji,
- `Nie dotyczy` — świadomie poza zakresem z uzasadnieniem.

## Minimum dla Release Candidate

- [ ] signed AAB buduje się,
- [ ] CI i testy są zielone,
- [ ] brak P0/P1,
- [ ] czysta instalacja i aktualizacja działają,
- [ ] logowanie, rejestracja i reset hasła działają,
- [ ] Start, Mapa, Lista, szczegóły, ranking i profil działają,
- [ ] dodawanie miejsca, opinii i zdjęć działa,
- [ ] brak internetu jest obsłużony,
- [ ] brak lokalizacji, kamery i powiadomień nie blokuje aplikacji,
- [ ] podstawowa accessibility jest sprawdzona,
- [ ] Crashlytics i App Check są zweryfikowane.

## Minimum dla publicznego release

- [ ] Data Safety zapisane w Play Console,
- [ ] Privacy Policy URL działa,
- [ ] Account deletion URL działa,
- [ ] account deletion ma PASS,
- [ ] runtime permissions mają PASS,
- [ ] manifest nie ma `ACCESS_BACKGROUND_LOCATION`,
- [ ] manifest nie ma `READ_MEDIA_IMAGES` ani `READ_EXTERNAL_STORAGE`,
- [ ] Photo Picker działa bez szerokiej zgody do galerii,
- [ ] Firestore Rules i Storage Rules są wdrożone,
- [ ] release nie loguje danych wrażliwych,
- [ ] widget i cache nie ujawniają danych po logout/delete,
- [ ] staged rollout i monitoring są przygotowane.

## Security i privacy

- [ ] App Check release używa Play Integrity,
- [ ] debug provider nie działa w release,
- [ ] klucz Maps jest ograniczony,
- [ ] tokeny FCM są prywatne,
- [ ] Analytics, Performance i Crashlytics odpowiadają Data Safety,
- [ ] PII nie trafia do eventów, logów, breadcrumbs ani custom keys,
- [ ] usuwanie konta czyści lub anonimizuje dane zgodnie z dokumentami.

## Runtime permissions

- [ ] allow approximate i precise działa,
- [ ] pierwsza odmowa jest obsłużona,
- [ ] kolejna odmowa nie tworzy martwego przycisku,
- [ ] trwała odmowa lokalizacji prowadzi do ustawień,
- [ ] trwała odmowa kamery prowadzi do ustawień,
- [ ] powrót z ustawień odświeża stan,
- [ ] aplikacja działa bez zgód opcjonalnych.

## Release blockers

NO-GO, gdy występuje co najmniej jeden punkt:

- crash przy starcie,
- brak logowania lub rejestracji,
- brak mapy lub listy,
- brak możliwości dodania miejsca,
- krytyczny błąd Rules,
- niepoprawny signed AAB,
- Data Safety niezgodne z aplikacją,
- account deletion nie działa,
- martwe akcje po odmowie uprawnień,
- dane prywatne widoczne po logout/delete,
- aktywny debug provider w release,
- krytyczna luka security lub privacy.

## Decyzja GO

GO wymaga:

- wszystkich punktów minimum ze statusem `OK`,
- braku aktywnych blockerów,
- PASS dla manualnego smoke testu,
- PASS dla account deletion,
- PASS dla runtime permissions,
- PASS dla security checklist,
- akceptacji właściciela projektu.

## Podpis

```text
Data:
Wersja:
Build / commit:
Track:
Osoba sprawdzająca:
Runtime permissions: PASS / FAIL / BLOCKED
Account deletion: PASS / FAIL / BLOCKED
Data Safety: PASS / FAIL / BLOCKED
Security checklist: PASS / FAIL / BLOCKED
Decyzja: GO / NO-GO
Uwagi:
Dowody:
```
