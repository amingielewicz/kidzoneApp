# Backups Handbook

## Cel

Dokument opisuje zasady wykonywania i weryfikacji kopii zapasowych dla KidZone.

## Zakres

Kopie zapasowe powinny obejmować:

- dane Firestore,
- pliki Storage,
- konfigurację Firebase,
- dokumentację projektu,
- kluczowe sekrety przechowywane poza repo.

## Zasady

- Backup musi być wykonywany regularnie.
- Backup musi być możliwy do odtworzenia.
- Backup powinien być przechowywany w bezpiecznej lokalizacji.
- Dostęp do kopii mają tylko uprawnione osoby.

## Test odtworzenia

Co najmniej okresowo należy potwierdzić, że kopię można odtworzyć.

Checklist:

- [ ] Backup został wykonany.
- [ ] Backup został zweryfikowany.
- [ ] Odtworzenie zakończyło się sukcesem.
- [ ] Wynik testu został udokumentowany.

## Retencja

Należy zdefiniować:

- częstotliwość wykonywania,
- okres przechowywania,
- sposób usuwania starych kopii,
- odpowiedzialność za proces.

## Incydenty

Po każdym incydencie dotyczącym utraty danych należy sprawdzić:

- [ ] aktualność kopii,
- [ ] możliwość odtworzenia,
- [ ] kompletność danych,
- [ ] czas potrzebny na odtworzenie.
