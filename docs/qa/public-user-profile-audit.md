# Public user profile audit

Powiazane issue: #280

## Cel

Zweryfikować, że publiczne dokumenty `users/{uid}` nie zawierają prywatnych
danych użytkowników. Publiczne dokumenty mogą być czytane przez zalogowanych
użytkowników, więc nie wolno w nich trzymać PII ani danych technicznych typu FCM.

## Pola dozwolone w `users/{uid}`

Publiczny profil powinien zawierać tylko dane potrzebne do widoku społeczności:

- `id`,
- `name`,
- `nameLowercase`,
- `role`,
- `placesAddedCount`,
- `reviewsCount`,
- `badgeEarnedAt`,
- `banReason`,
- `bannedUntilMillis`,
- `createdAtMillis`,
- `updatedAtMillis`.

## Pola zakazane w `users/{uid}`

Te dane muszą być w dokumentach prywatnych albo nie powinny być zapisywane:

- `email`,
- `firstName`,
- `lastName`,
- `emailNotificationsEnabled`,
- `fcmTokens`,
- `phone`,
- `address`,
- `notificationPrefs`,
- `privateSettings`.

Docelowe ścieżki:

```text
users/{uid}/private/profile
users/{uid}/private/messaging
```

## Weryfikacja w Firebase Console

1. Otwórz Firebase Console.
2. Wejdź w Firestore Database.
3. Otwórz kolekcję `users`.
4. Sprawdź kilka nowych i starych dokumentów użytkowników.
5. Potwierdź, że zakazane pola nie występują w dokumencie publicznym.
6. Dla wybranego użytkownika otwórz `private/profile` i `private/messaging`.
7. Potwierdź, że prywatne dane są tylko w subkolekcji `private`.

## Migracja starych danych

Jeśli w `users/{uid}` występują prywatne pola:

1. Skopiuj wartości do właściwego dokumentu prywatnego.
2. Usuń prywatne pola z dokumentu publicznego.
3. Zweryfikuj aplikację po migracji na koncie testowym.
4. Nie usuwaj dokumentu publicznego w całości, jeśli zawiera liczniki lub role.

## Testy automatyczne

Reguły blokują tworzenie i aktualizację publicznego profilu z prywatnymi polami.

Komenda:

```powershell
firebase emulators:exec --only firestore,storage "npm --prefix tests/firestore-rules test"
```

## Kryterium PASS

- `users/{uid}` nie zawiera zakazanych pól,
- `users/{uid}/private/profile` jest czytelny tylko dla ownera albo admina,
- `users/{uid}/private/messaging` jest czytelny tylko dla ownera albo admina,
- testy emulatora przechodzą zielono.
