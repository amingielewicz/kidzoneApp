# Test Plan

Ostatnia aktualizacja: 2026-07-14

## Cel

Plan testów dla release candidate kidZone. Dokument określa zakres, środowiska, odpowiedzialności, kryteria wejścia i wyjścia oraz wymagane dowody.

## Metadane testu

```text
Version name / code:
Commit / tag:
Build artifact:
Firebase project:
Google Play track:
QA owner:
Release owner:
Devices:
Android versions:
Start date:
End date:
Final result: PASS / FAIL / BLOCKED
```

## Kryteria wejścia

- [ ] CI jest zielone,
- [ ] signed build jest dostępny,
- [ ] backend, Rules, indeksy i Remote Config są na właściwym środowisku,
- [ ] dane testowe są przygotowane,
- [ ] znane blokery są zamknięte albo świadomie zaakceptowane,
- [ ] zakres zmian i ryzyka są znane,
- [ ] tester ma dostęp do wymaganych konsol i urządzeń.

## Zakres funkcjonalny

### Konto i auth

- logowanie, rejestracja i reset hasła,
- sesja po restarcie,
- logout,
- ban sign-out,
- account deletion,
- reauthentication dla operacji wrażliwych.

### Miejsca i opinie

- dodawanie, edycja i usuwanie miejsc,
- lista, wyszukiwanie, filtry i sortowanie,
- szczegóły miejsca,
- opinie, oceny i zgłoszenia,
- walidacja i double submit,
- usunięty lub niedostępny zasób.

### Zdjęcia

- Photo Picker,
- kamera,
- odmowa uprawnienia,
- kompresja i upload,
- zły MIME lub rozmiar,
- przerwanie sieci,
- retry bez duplikatu,
- cleanup Storage.

### Mapa i lokalizacja

- mapa, markery, clustering i bounds,
- fallback listowy,
- approximate i precise location,
- zwykła oraz trwała odmowa,
- wyłączony GPS,
- powrót z ustawień,
- brak nadmiarowych requestów.

### Profil, ranking i widget

- profil publiczny i prywatny,
- ranking oraz pola agregowane,
- odznaki,
- ustawienia,
- widget bez cache i lokalizacji,
- cleanup widgetu po logout/delete.

### Backend i integracje

- Cloud Functions,
- Firestore i Storage Rules,
- App Check,
- FCM i deep linki,
- Remote Config,
- Analytics, Crashlytics i Performance,
- panel administracyjny.

### Offline

- cache Room,
- offline bez cache,
- zapis offline nie udaje sukcesu,
- retry i brak phantom records,
- WorkManager i dead-letter, jeśli testowane.

## Rodzaje testów

- unit,
- integration,
- UI/instrumentation,
- smoke,
- regression,
- exploratory,
- accessibility,
- performance,
- security i privacy,
- release validation.

## Środowiska

### Local / emulator

Do testów jednostkowych, integracyjnych, Rules i szybkiej diagnostyki.

### Test Firebase project

Do bezpiecznych testów backendu, App Check, Storage, Functions, obciążenia i migracji.

### Google Play testing track

Do signed builda, Play Integrity, aktualizacji, dystrybucji i finalnego smoke.

### Production

Wyłącznie monitoring, staged rollout i kontrolowane scenariusze bez generowania sztucznych danych lub ryzyka dla użytkowników.

## Macierz urządzeń

Uwzględnij:

- minimalne wspierane API,
- najnowsze wspierane API,
- co najmniej jedną wersję pośrednią,
- mały i większy ekran,
- urządzenie fizyczne,
- różne ustawienia font/display size,
- launcher wspierający widget.

## Scenariusze negatywne

- brak internetu i timeout,
- wygasła sesja,
- odmowa uprawnień,
- wyłączony GPS,
- błąd Rules lub App Check,
- nieważny deep link,
- częściowy upload lub cleanup,
- równoległe akcje,
- stary build podczas migracji,
- nieważny token FCM.

## Zarządzanie błędami

Każdy błąd powinien zawierać:

- kroki,
- wynik aktualny i oczekiwany,
- build i środowisko,
- urządzenie,
- dowody,
- severity i priority,
- informację o regresji,
- wpływ na GO / NO-GO.

## Kryteria wyjścia

- brak P0/P1,
- krytyczne flow mają PASS,
- smoke i wymagany zakres regresji mają PASS,
- runtime permissions mają PASS,
- account deletion ma PASS,
- signed build, Rules i App Check są zweryfikowane,
- znane ryzyka mają właściciela,
- wyniki i dowody są zapisane,
- GO / NO-GO zostało zakończone.

## Raport końcowy

```text
Executed:
Passed:
Failed:
Blocked:
Not applicable:
Open defects:
Accepted risks:
Evidence:
Recommendation: GO / NO-GO
```
