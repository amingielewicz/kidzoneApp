# Firebase App Check

Ostatnia aktualizacja: 2026-07-13

## Cel

App Check ogranicza dostęp do Firebase z niezaufanych klientów. W kidZone:

- debug build używa `DebugAppCheckProviderFactory`,
- release build używa `PlayIntegrityAppCheckProviderFactory`.

Konfiguracja aplikacji znajduje się w `KidZoneApplication.initAppCheck()`.

## Debug provider

Debug provider służy wyłącznie do developmentu, testów i emulatorów.

1. Uruchom debug build.
2. Odszukaj debug token w Logcat.
3. Dodaj go w Firebase Console do właściwej aplikacji Android.
4. Nadaj tokenowi nazwę identyfikującą urządzenie i właściciela.
5. Usuń nieużywane tokeny.

Debug token:

- nie trafia do repo,
- nie jest kopiowany do dokumentacji ani issue,
- nie jest używany w release,
- powinien być rotowany po ujawnieniu.

## Release provider

Przed testem release sprawdź:

- poprawny package name,
- właściwy projekt Firebase,
- SHA-1 i SHA-256 wymaganych certyfikatów,
- zgodność podpisu lokalnego i Google Play App Signing,
- działanie Firestore, Storage i Cloud Functions,
- brak debug providera i debug tokenów.

Play Integrity nie zwalnia z Firestore Rules, Storage Rules, auth checks ani rate limitingu.

## Monitoring przed enforcement

Przed blokowaniem ruchu:

- obserwuj udział poprawnych, niepoprawnych i niezweryfikowanych requestów,
- zweryfikuj debug, internal testing i release candidate,
- sprawdź wszystkie aktywne wersje aplikacji,
- upewnij się, że backend i funkcje nie są błędnie blokowane,
- przygotuj rollback enforcement.

## Kolejność rollout

1. Skonfiguruj debug tokeny.
2. Zweryfikuj debug build.
3. Zweryfikuj signed release build lub build z Internal Testing.
4. Włącz monitoring bez enforcement, jeśli usługa to wspiera.
5. Włącz enforcement dla jednej usługi.
6. Wykonaj smoke i sprawdź logi.
7. Obserwuj błędy oraz metryki.
8. Dopiero potem rozszerz enforcement.

Nie włączamy enforcement dla wszystkich usług jednocześnie.

## Minimalny smoke

Sprawdź:

- logowanie i rejestrację,
- Start, Listę, Mapę, Ranking i Profil,
- odczyt danych z Firestore,
- dodanie miejsca i opinii,
- upload zdjęcia do Storage,
- funkcje callable/HTTP używane przez aplikację,
- account deletion, jeśli zależy od funkcji backendowej,
- zachowanie po restarcie i ponownym zalogowaniu.

PASS oznacza:

- brak `App attestation failed`,
- brak nowych `PERMISSION_DENIED` wynikających z App Check,
- brak blokady prawidłowego release builda,
- brak nowych krytycznych błędów w Crashlytics i logach backendu.

## Scenariusze negatywne

- debug build bez zarejestrowanego tokenu,
- stary lub usunięty debug token,
- release build podpisany innym certyfikatem,
- aplikacja spoza Google Play, jeśli dystrybucja jej nie zakłada,
- brak internetu podczas pobierania tokenu,
- chwilowy błąd Play Integrity,
- wyłączony enforcement po incydencie.

Aplikacja powinna pokazywać kontrolowany błąd i możliwość retry, a nie techniczny wyjątek.

## Bezpieczeństwo

- tokenów App Check nie logujemy,
- nie traktujemy App Check jako uwierzytelnienia użytkownika,
- funkcje nadal sprawdzają auth i role,
- Rules nadal sprawdzają ownership i pola,
- błędy App Check nie zawierają danych użytkownika w telemetryce.

## Release gate

- [ ] debug provider nie działa w release,
- [ ] Play Integrity jest aktywne,
- [ ] certyfikaty są poprawne,
- [ ] signed build przeszedł smoke,
- [ ] enforcement jest włączony tylko dla zweryfikowanych usług,
- [ ] monitoring i rollback są przygotowane,
- [ ] Data Safety i dokumentacja bezpieczeństwa odpowiadają konfiguracji.
