# Regression Testing

Ostatnia aktualizacja: 2026-07-14

## Cel

Zakres testów regresyjnych wykonywanych przed releasem oraz po zmianach wpływających na istniejącą funkcjonalność kidZone.

## Kiedy wykonywać

- przed każdym release candidate,
- po hotfixie,
- po refaktoryzacji kodu współdzielonego,
- po zmianach Firebase, Rules, indeksów lub App Check,
- po zmianach nawigacji i auth,
- po aktualizacji bibliotek lub target SDK,
- po migracji schematu lub cache,
- po zmianach uprawnień, account deletion albo Data Safety.

## Dobór zakresu

Regresja powinna obejmować:

1. bezpośrednio zmienioną funkcję,
2. funkcje korzystające z tego samego komponentu lub danych,
3. krytyczne ścieżki release,
4. scenariusze negatywne związane z ryzykiem zmiany.

## Authentication i konto

- logowanie i rejestracja,
- reset hasła,
- restart sesji,
- logout i czyszczenie lokalnego stanu,
- ban sign-out,
- account deletion,
- brak powrotu Back do prywatnego ekranu.

## Miejsca, lista i wyszukiwanie

- dodawanie i edycja miejsca,
- walidacja oraz double submit,
- lista, wyszukiwanie, filtry i sortowanie,
- paginacja bez duplikatów,
- puste wyniki,
- cache i brak internetu,
- szczegóły miejsca i usunięty zasób.

## Opinie i zdjęcia

- dodawanie, edycja, usuwanie i zgłaszanie opinii,
- zakres oceny i limity tekstu,
- Photo Picker,
- kamera i odmowa uprawnienia,
- upload, retry i częściowy błąd,
- usunięcie zdjęcia oraz cleanup Storage.

## Mapa i lokalizacja

- ładowanie mapy,
- bounds, markery i clustering,
- przejście marker → szczegóły,
- fallback listowy,
- approximate i precise location,
- zwykła oraz trwała odmowa,
- wyłączony GPS,
- powrót z ustawień,
- brak lawiny requestów podczas ruchu mapy.

## Ranking i profil

- ranking miejsc i użytkowników,
- puste stany,
- pola agregowane,
- profil publiczny i prywatny,
- odznaki,
- ustawienia,
- duża czcionka i TalkBack.

## Backend i Firebase

- Firestore Rules,
- Storage Rules,
- indeksy,
- App Check,
- Cloud Functions,
- FCM i deep linki,
- idempotencja eventów,
- rate limiting,
- cleanup account deletion,
- monitoring błędów i kosztów.

## System i release

- czysta instalacja,
- aktualizacja z poprzedniej wersji,
- signed AAB,
- różne wersje Androida,
- runtime permissions,
- widget,
- procesy background i WorkManager,
- Crashlytics, Performance i Android vitals.

## Scenariusze negatywne

- brak internetu i timeout,
- wygasła sesja,
- odmowa uprawnień,
- wyłączony GPS,
- błąd Rules/App Check,
- nieważny deep link,
- częściowy upload lub cleanup,
- równoległe akcje,
- starszy build podczas migracji,
- nieważny token FCM.

## Kryteria zakończenia

- brak regresji w funkcjach krytycznych,
- brak otwartych P0/P1,
- wszystkie wymagane smoke testy mają PASS,
- runtime permissions i account deletion mają PASS,
- wyniki i dowody są zapisane,
- znane ryzyka mają właściciela i świadomą akceptację,
- GO / NO-GO zostało zakończone.

## Raport

```text
Zakres zmiany:
Wersja / build:
Urządzenia:
Zakres regresji:
PASS:
FAIL:
BLOCKED:
Nowe błędy:
Znane ryzyka:
Dowody:
Decyzja:
```
