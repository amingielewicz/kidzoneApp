# Firebase Operations Handbook

## Cel

Dokument opisuje operacyjne zasady utrzymania Firebase dla KidZone.

## Obszary odpowiedzialności

Firebase w KidZone obejmuje:

- Authentication,
- Firestore,
- Storage,
- Cloud Functions,
- Remote Config,
- App Check,
- Crashlytics,
- Performance,
- Analytics,
- Cloud Messaging.

## Firestore

### Przed release

- [ ] Rules wdrożone na właściwy projekt.
- [ ] Rules testy przechodzą.
- [ ] Indeksy wymagane przez zapytania istnieją.
- [ ] Zapytania list i rankingów mają limity.
- [ ] Publiczne dokumenty nie zawierają PII.
- [ ] Dane prywatne są oddzielone od publicznych.

### Po release

- [ ] Sprawdzić usage.
- [ ] Sprawdzić błędy rules.
- [ ] Sprawdzić nietypowy wzrost odczytów.
- [ ] Sprawdzić koszty.

## Storage

### Przed release

- [ ] Rules wdrożone.
- [ ] Upload zdjęcia działa.
- [ ] Rozmiar pliku jest walidowany.
- [ ] Typ pliku jest walidowany.
- [ ] EXIF/GPS są obsłużone zgodnie z decyzją privacy.

### Po release

- [ ] Sprawdzić storage usage.
- [ ] Sprawdzić błędy uploadu.
- [ ] Sprawdzić koszty transferu.

## Cloud Functions

- [ ] Każda funkcja modyfikująca dane ma auth check.
- [ ] Funkcje admina nie ufają danym z klienta.
- [ ] Funkcje mają testy.
- [ ] Błędy funkcji są monitorowane.
- [ ] Timeouty i retry są świadomie ustawione.

## App Check

- [ ] Debug provider nie działa w release.
- [ ] Enforcement jest włączony świadomie.
- [ ] Błędy App Check są monitorowane.
- [ ] Release build działa z App Check.

## Remote Config

Parametry powinny mieć:

- wartość domyślną w aplikacji,
- wartość produkcyjną w Firebase,
- opis celu,
- bezpieczny fallback.

## Backup i eksport

Dla danych produkcyjnych należy ustalić:

- częstotliwość eksportu,
- miejsce przechowywania,
- dostęp osób uprawnionych,
- procedurę odtworzenia,
- test odtworzenia.

## Incident checklist

- [ ] Ustalić, której usługi dotyczy incydent.
- [ ] Sprawdzić Firebase Status.
- [ ] Sprawdzić ostatnie deploye.
- [ ] Sprawdzić billing.
- [ ] Sprawdzić Crashlytics / Functions logs.
- [ ] Zdecydować: rollback, hotfix, config change albo monitorowanie.
