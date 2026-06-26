# Firebase App Check

## Cel

App Check ogranicza dostęp do Firebase z niezaufanych klientów. W KidZone używamy:

- `DebugAppCheckProviderFactory` dla buildów debug,
- `PlayIntegrityAppCheckProviderFactory` dla buildów release.

Konfiguracja kodowa znajduje się w `KidZoneApplication.initAppCheck()`.

## Debug provider

Debug provider jest wymagany do pracy lokalnej i testów na emulatorze, szczególnie jeśli w Firebase Console zostanie włączony App Check enforcement.

Kroki:

1. Uruchom aplikację w wariancie debug.
2. Otwórz Logcat i wyszukaj:

```text
DebugAppCheckProvider
```

3. Skopiuj debug token wypisany przez Firebase SDK.
4. W Firebase Console przejdź do:

```text
Project settings -> App Check -> Apps -> Android app -> Manage debug tokens
```

5. Dodaj token z czytelną nazwą, np.:

```text
Adam emulator Pixel API 35
```

Nie commituj debug tokenów do repozytorium.

## Release provider

Release build używa Play Integrity. Przed włączeniem enforcement w produkcji upewnij się, że:

- aplikacja ma poprawny package name w Firebase,
- podpis release jest zgodny z konfiguracją Google Play / Firebase,
- SHA certyfikatów jest zarejestrowany tam, gdzie wymaga tego konfiguracja projektu,
- build release komunikuje się z Firestore, Storage i Cloud Functions bez błędów App Check.

## Enforcement

Nie włączaj enforcement dla wszystkich usług naraz bez smoke testu.

Zalecana kolejność:

1. Zarejestruj debug tokeny dla urządzeń deweloperskich.
2. Uruchom debug build i sprawdź logowanie, listę miejsc, mapę, dodawanie miejsca i upload zdjęć.
3. Włącz enforcement najpierw dla jednej usługi o najmniejszym ryzyku.
4. Zweryfikuj Crashlytics i logi Firebase.
5. Dopiero potem rozszerz enforcement na kolejne usługi.

### Minimalny smoke test enforcement

W Firebase Console przejdz do:

```text
Build -> App Check -> Apps -> Android app
```

Przed zmiana trybu enforcement:

1. Upewnij sie, ze debug token aktualnego urzadzenia jest dodany.
2. Uruchom aplikacje i zaloguj testowego uzytkownika.
3. Otworz Start, Liste, Mape, Ranking i Profil.
4. Dodaj testowe miejsce bez zdjec, potem dodaj jedno zdjecie.
5. Dodaj opinie z jednym zdjeciem.

Po wlaczeniu enforcement dla pojedynczej uslugi powtorz smoke test:

- Firestore: logowanie, profil, lista miejsc, dodanie miejsca, opinia.
- Storage: upload zdjec miejsca i opinii.
- Cloud Functions: funkcje wywolywane przez aplikacje, jesli sa objete App Check.

PASS oznacza brak `PERMISSION_DENIED`, `App attestation failed` i brak nowych bledow
blokujacych flow w Crashlytics.

## Checklist

- [ ] Debug token dodany w Firebase Console.
- [ ] Debug build nie pokazuje `App attestation failed`.
- [ ] Release build używa Play Integrity.
- [ ] Firestore działa po włączeniu enforcement.
- [ ] Storage upload działa po włączeniu enforcement.
- [ ] Cloud Functions działają po włączeniu enforcement, jeśli są objęte App Check.
