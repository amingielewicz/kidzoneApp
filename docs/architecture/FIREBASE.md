# Firebase Handbook

## Cel

Dokument opisuje zasady korzystania z Firebase w KidZone.

## Używane usługi

- Firebase Authentication.
- Firestore.
- Storage.
- Cloud Functions.
- Crashlytics.
- Analytics.
- Performance Monitoring.
- Remote Config.
- App Check.
- Cloud Messaging.

## Authentication

Zasady:

- Operacje zależne od użytkownika muszą wymagać zalogowania.
- UID użytkownika jest podstawowym identyfikatorem właściciela danych.
- Dane publiczne i prywatne muszą być rozdzielone.
- Usunięcie konta musi obsłużyć dane zależne od użytkownika zgodnie z polityką prywatności.

## Firestore

Zasady:

- Nie pobieramy całych kolekcji bez limitu.
- Zapytania list i rankingów muszą mieć limit.
- Zapytania sortowane muszą mieć indeksy.
- Pola rankingowe i licznikowe powinny być denormalizowane, jeśli poprawia to koszt i wydajność.
- Publiczne dokumenty użytkownika nie powinny zawierać PII.

## Storage

Zasady:

- Upload zdjęć ma walidację typu i rozmiaru.
- Zdjęcia są kompresowane przed wysłaniem.
- EXIF/GPS powinny być usuwane, jeśli nie są potrzebne.
- Storage Rules muszą weryfikować właściciela i kontekst operacji.

## Cloud Functions

Zasady:

- Każda funkcja modyfikująca dane ma auth check.
- Funkcje admina nie ufają danym z klienta.
- Funkcje powinny logować metryki, nie dane wrażliwe.
- Operacje kosztowne powinny mieć limity i walidację.

## App Check

Przed publicznym release:

- [ ] App Check działa na release buildzie.
- [ ] Debug provider nie działa w release.
- [ ] Enforcement jest świadomie włączony dla właściwych usług.
- [ ] Błędy App Check są monitorowane.

## Remote Config

Remote Config może sterować:

- limitami listy,
- limitem markerów,
- debounce wyszukiwarki,
- debounce mapy,
- maintenance mode,
- feature flags,
- A/B testingiem.

## Crashlytics

Wymagania:

- testowy crash widoczny przed release,
- release build raportuje błędy,
- logi nie zawierają danych wrażliwych,
- krytyczne crashe tworzą follow-up issue.

## Performance

Trace powinny obejmować:

- cold start,
- ładowanie miejsc,
- mapę,
- ranking,
- wyszukiwarkę,
- dodawanie miejsca,
- upload zdjęcia.

## Checklist

- [ ] Firestore Rules sprawdzone.
- [ ] Storage Rules sprawdzone.
- [ ] App Check zweryfikowany.
- [ ] Crashlytics zweryfikowany.
- [ ] Remote Config ma wartości produkcyjne.
- [ ] Nie ma sekretów w repo.
- [ ] Zapytania mają limity.
