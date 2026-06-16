# Firebase Security Hardening Plan

## Cel

Ten dokument opisuje plan utwardzenia Firebase w KidZone bez ryzykownego zmieniania reguł produkcyjnych na ślepo.

## Aktualny stan

Projekt korzysta z:

- Firestore Rules,
- Storage Rules,
- Firebase App Check,
- Firebase Hosting,
- Firebase Auth,
- Firebase Cloud Functions,
- Firebase Cloud Messaging.

## Najważniejsze ustalenia

### 1. Publiczne i prywatne dane użytkownika są w jednym dokumencie

Dokument `users/{uid}` zawiera jednocześnie dane publiczne i prywatne.

Przykładowe pola publiczne:

- `avatarUrl`,
- `name`,
- `nameLowercase`,
- `lastKnownUserRank`,
- `placesAddedCount`,
- `reviewsCount`,
- `badgeEarnedAt`.

Przykładowe pola prywatne:

- `email`,
- `fcmTokens`.

Ryzyko:

- Firestore nie ukrywa pojedynczych pól w odczytanym dokumencie.
- Jeśli dokument jest czytelny, klient dostaje cały dokument.
- `email` i `fcmTokens` nie powinny być widoczne dla innych użytkowników.

Docelowy kierunek:

```text
users/{uid}
users/{uid}/private/profile
```

`users/{uid}` powinien zawierać tylko profil publiczny.

`users/{uid}/private/profile` powinien zawierać dane prywatne.

### 2. Upload zdjęć wymaga dalszego utwardzenia

Storage Rules ograniczają typ i rozmiar pliku, ale docelowo upload powinien być powiązany z właścicielem albo autorem.

Docelowy kierunek:

```text
places/{placeId}/users/{uid}/{fileName}
reviews/{reviewId}/users/{uid}/{fileName}
```

Dzięki temu reguła może sprawdzić, czy:

```text
request.auth.uid == uid
```

### 3. Rola admina musi bazować na custom claims

Dostęp administratorski powinien zależeć od Firebase custom claims, a nie od pola `role` w dokumencie użytkownika.

Zasada:

```text
request.auth.token.admin == true
```

Pole `role` może istnieć informacyjnie w Firestore, ale nie powinno być źródłem prawdy dla dostępu admina.

### 4. App Check wymaga kontrolowanego rollout'u

Aplikacja inicjalizuje Firebase App Check w `KidZoneApplication`:

- debug build używa debug providera,
- release build używa Play Integrity.

Procedura rejestracji debug tokenów i włączania enforcement jest opisana w [`docs/app-check.md`](app-check.md).

Nie należy włączać enforcement bez wcześniejszego smoke testu logowania, Firestore, Storage i Cloud Functions.

## Kolejność wdrożenia

### Etap 1 — dokumentacja i testy

- opisać aktualne ryzyka,
- udokumentować App Check i debug tokeny,
- dopisać testy Firestore Rules,
- dopisać lub przygotować testy Storage Rules,
- upewnić się, że CI testuje reguły.

### Etap 2 — migracja danych użytkownika

- dodać obsługę prywatnego subdokumentu,
- przenieść `email` i `fcmTokens`,
- zmienić odczyty profili publicznych,
- dopiero potem zaostrzyć Firestore Rules.

### Etap 3 — migracja ścieżek zdjęć

- zmienić ścieżki uploadu zdjęć,
- dodać `uid` do ścieżki,
- zaostrzyć Storage Rules,
- dopisać testy odmowy uploadu do cudzych ścieżek.

## Czego nie robić bez migracji

Nie należy od razu blokować odczytu `users/{uid}`, bo aplikacja może zależeć od publicznych danych profilu.

Nie należy od razu zmieniać Storage Rules na właścicielskie, jeśli aplikacja nadal zapisuje zdjęcia pod starymi ścieżkami.

Nie należy usuwać pól `email` ani `fcmTokens` ręcznie z Firebase Console.

## Checklist przed zmianą reguł

- [ ] Wiemy, które ekrany czytają `users/{uid}`.
- [ ] Wiemy, gdzie aplikacja zapisuje `fcmTokens`.
- [ ] Wiemy, gdzie aplikacja czyta `email`.
- [ ] Debug tokeny App Check są dodane w Firebase Console.
- [ ] App Check enforcement został sprawdzony smoke testem.
- [ ] Mamy testy odmowy odczytu prywatnych danych.
- [ ] Mamy testy odmowy uploadu do cudzej ścieżki.
- [ ] Android CI jest zielony.
- [ ] Firestore Rules Tests są zielone.

## Status

Ten dokument jest planem technicznym. Nie zmienia działania aplikacji ani produkcyjnych reguł Firebase.
