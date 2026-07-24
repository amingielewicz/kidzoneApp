# Smoke Tests

Ostatnia aktualizacja: 2026-07-14

## Cel

Minimalny zestaw testów wykonywanych na właściwym buildzie po wdrożeniu i przed udostępnieniem go szerszej grupie użytkowników.

## Warunki

- signed release build lub build z właściwego tracku,
- właściwy projekt Firebase,
- aktywne produkcyjne Rules, indeksy i konfiguracja,
- konto testowe bez danych produkcyjnych,
- co najmniej jedno fizyczne urządzenie dla release candidate.

## Krytyczne scenariusze

### Start i auth

- [ ] czysta instalacja uruchamia się bez crasha,
- [ ] aktualizacja z poprzedniej wersji działa,
- [ ] logowanie działa,
- [ ] rejestracja działa,
- [ ] reset hasła ma kontrolowany wynik,
- [ ] restart aplikacji zachowuje właściwy stan sesji.

### Główne ekrany

- [ ] Start pokazuje dane albo kontrolowany pusty stan,
- [ ] Lista ładuje miejsca i otwiera szczegóły,
- [ ] wyszukiwanie, filtr i sortowanie działają,
- [ ] Mapa pokazuje dane albo fallback listowy,
- [ ] Ranking i Profil działają.

### Treści użytkownika

- [ ] można dodać miejsce bez zdjęcia,
- [ ] można dodać miejsce ze zdjęciem,
- [ ] można dodać opinię,
- [ ] Photo Picker działa bez szerokiej zgody galerii,
- [ ] double submit nie tworzy duplikatu.

### Uprawnienia i system

- [ ] aplikacja działa bez lokalizacji,
- [ ] „Moja lokalizacja” obsługuje zgodę i odmowę,
- [ ] trwała odmowa prowadzi do ustawień,
- [ ] powrót z ustawień odświeża stan,
- [ ] odmowa kamery i powiadomień nie blokuje aplikacji.

### Offline i błędy

- [ ] brak internetu nie powoduje crasha,
- [ ] cache pozostaje użyteczny,
- [ ] zapis offline nie udaje sukcesu,
- [ ] retry nie tworzy duplikatu,
- [ ] komunikaty nie pokazują surowych wyjątków.

### Konto i prywatność

- [ ] logout czyści prywatny stan, cache i widget,
- [ ] account deletion działa na koncie testowym,
- [ ] po logout/delete Back nie wraca do prywatnego ekranu,
- [ ] token FCM i lokalny stan są czyszczone zgodnie z projektem.

### Backend i monitoring

- [ ] Firestore i Storage nie zwracają nowych błędów Rules,
- [ ] App Check nie blokuje prawidłowego builda,
- [ ] Cloud Functions krytyczne działają,
- [ ] deep link i powiadomienie otwierają właściwy ekran,
- [ ] Crashlytics i Android vitals nie pokazują nowego krytycznego błędu.

## Wynik

Każdy scenariusz otrzymuje status:

- `PASS`,
- `FAIL`,
- `BLOCKED`,
- `NOT APPLICABLE` z uzasadnieniem.

## Kryteria GO

- brak `FAIL` dla scenariuszy krytycznych,
- brak P0/P1,
- account deletion i runtime permissions mają PASS,
- monitoring nie pokazuje regresji,
- dowody zostały zapisane,
- decyzja GO / NO-GO jest świadoma.

## Raport

```text
Data:
Wersja / build:
Commit:
Track:
Urządzenie i Android:
Tester:
Wynik: PASS / FAIL / BLOCKED
Nieudane scenariusze:
Dowody:
Decyzja:
```
