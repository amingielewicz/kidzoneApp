# Firebase Handbook

Ostatnia aktualizacja: 2026-07-13

## Cel

Zasady korzystania z Firebase w kidZone z uwzględnieniem bezpieczeństwa, kosztów, prywatności, synchronizacji i procesu release.

## Używane usługi

- Firebase Authentication,
- Cloud Firestore,
- Firebase Storage,
- Cloud Functions,
- Crashlytics,
- Analytics,
- Performance Monitoring,
- Remote Config,
- App Check,
- Cloud Messaging.

## Authentication

- operacje zależne od użytkownika wymagają zalogowania,
- UID jest identyfikatorem właściciela danych,
- dane publiczne i prywatne są rozdzielone,
- reauthentication jest obsłużone dla operacji wrażliwych,
- account deletion obejmuje Auth, dane, pliki, tokeny i lokalny stan.

## Firestore

- nie pobieramy całych kolekcji bez limitu,
- listy i rankingi mają limity oraz indeksy,
- pola agregowane są denormalizowane, gdy zmniejsza to koszt,
- publiczny profil nie zawiera PII,
- retry zapisów jest idempotentne,
- reguły blokują zmianę pól administracyjnych,
- zmiana schematu wymaga planu migracji i zgodności cache.

## Storage

- upload waliduje MIME, rozmiar i ownership,
- zdjęcia są kompresowane,
- EXIF i GPS są usuwane, jeśli nie są potrzebne,
- ścieżki są właścicielskie,
- cleanup jest zgodny z account deletion,
- aplikacja wybiera zdjęcia przez Android Photo Picker,
- brak `READ_MEDIA_IMAGES` i `READ_EXTERNAL_STORAGE`.

## Cloud Functions

- funkcje modyfikujące dane sprawdzają auth i rolę,
- dane z klienta nie są zaufane,
- operacje kosztowne mają limity,
- funkcje są odporne na retry i duplikaty,
- logi nie zawierają PII,
- błędy częściowe są monitorowane,
- scheduled functions obsługują nieaktywne konta i wygasłe tokeny.

## App Check

- debug provider tylko dla debug buildów,
- release używa Play Integrity,
- enforcement jest włączany świadomie,
- błędy są monitorowane,
- prawidłowe buildy nie mogą być blokowane przez błędną konfigurację,
- debug tokeny nie trafiają do repo ani logów.

## Remote Config

Może sterować:

- limitami listy i mapy,
- debounce,
- maintenance mode,
- feature flags,
- limitami zdjęć i uploadu,
- eksperymentami UX.

Parametr krytyczny powinien mieć wartość domyślną, opis, właściciela i możliwość rollbacku.

## Crashlytics i Performance

- testowy crash jest widoczny przed release,
- custom keys i breadcrumbs nie zawierają PII,
- surowe wyjątki zawierające dane użytkownika nie są raportowane,
- trace mają neutralne nazwy,
- aktywne SDK są zgodne z Data Safety.

## Analytics

- eventy nie zawierają e-maili, treści opinii, dokładnej lokalizacji ani URI zdjęć,
- nazwy i parametry są stabilne,
- user properties są ograniczone,
- zakres zbierania odpowiada polityce prywatności.

## Cloud Messaging

- tokeny FCM są prywatne,
- logout i delete account usuwają lub unieważniają token,
- nieprawidłowe tokeny są sprzątane,
- odmowa `POST_NOTIFICATIONS` nie blokuje aplikacji,
- deep linki z powiadomień są walidowane.

## Koszty i limity

- zapytania mają limity,
- mapy i listy nie pobierają pełnych kolekcji,
- upload ma limit liczby i rozmiaru plików,
- billing i alerty są skonfigurowane,
- kosztowne funkcje mają rate limiting,
- anomalie usage są monitorowane.

## Checklista

- [ ] Firestore Rules wdrożone i przetestowane,
- [ ] Storage Rules wdrożone i przetestowane,
- [ ] App Check zweryfikowany,
- [ ] Crashlytics, Analytics i Performance zgodne z Data Safety,
- [ ] Remote Config ma wartości produkcyjne,
- [ ] FCM tokeny są prywatne,
- [ ] account deletion czyści zależne dane,
- [ ] brak sekretów w repo,
- [ ] zapytania i uploady mają limity,
- [ ] alerty kosztowe działają.
