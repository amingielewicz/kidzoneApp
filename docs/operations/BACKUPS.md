# Backups Handbook

Ostatnia aktualizacja: 2026-07-13

## Cel

Zasady wykonywania, przechowywania i testowania kopii zapasowych kidZone.

## Zakres

Backup powinien obejmować:

- dane Firestore wymagające odtworzenia,
- pliki Firebase Storage,
- konfigurację Firebase i Rules,
- konfigurację Cloud Functions,
- dokumentację projektu i release,
- keystore oraz dane potrzebne do odzyskania podpisu,
- listę nazw sekretów i procedurę ich odtworzenia,
- eksport istotnych ustawień Google Play i Remote Config.

Sekrety nie mogą być przechowywane w plain text ani w repozytorium.

## Odpowiedzialność

Dla każdego rodzaju backupu należy określić:

- właściciela,
- częstotliwość,
- lokalizację,
- retencję,
- sposób szyfrowania,
- osoby z dostępem,
- procedurę odtworzenia.

## Zasady

- backup musi być regularny,
- kopia musi być oddzielona od środowiska produkcyjnego,
- dostęp jest zgodny z zasadą najmniejszych uprawnień,
- operacje backupu i restore są audytowalne,
- kopia bez sprawdzonego restore nie jest uznawana za skuteczną,
- retencja backupu musi odpowiadać polityce prywatności i usuwaniu danych.

## Dane użytkownika i retencja

Backup nie może bezterminowo omijać usuwania konta i retencji danych.

Należy ustalić:

- jak długo usunięte dane mogą pozostać w kopii,
- czy restore nie przywróci kont usuniętych po dacie backupu,
- jak ponownie zastosować operacje usunięcia po restore,
- jak chronione są dane osobowe w kopiach.

## Test odtworzenia

Test wykonujemy w odseparowanym środowisku.

- [ ] wskazano konkretną kopię,
- [ ] zweryfikowano integralność,
- [ ] odtworzono Firestore i wymagane pliki,
- [ ] sprawdzono Rules oraz dostęp,
- [ ] aplikacja testowa odczytuje dane,
- [ ] dane prywatne nie stały się publiczne,
- [ ] zmierzono czas odtworzenia,
- [ ] wynik i problemy udokumentowano.

## RPO i RTO

Należy jawnie ustalić:

- RPO — akceptowalną utratę danych w czasie,
- RTO — akceptowalny czas przywrócenia usługi.

Dla pierwszego release wartości mogą być uproszczone, ale muszą być zapisane i zaakceptowane.

## Incydent i restore

Przed restore:

1. określ zakres utraty lub uszkodzenia,
2. zabezpiecz dowody,
3. zatrzymaj dalsze nadpisywanie danych,
4. wybierz właściwą kopię,
5. sprawdź wpływ na dane usuniętych użytkowników,
6. wykonaj restore w kontrolowany sposób,
7. zweryfikuj integralność i bezpieczeństwo,
8. zapisz oś czasu oraz follow-up actions.

## Checklista okresowa

- [ ] backup wykonuje się zgodnie z harmonogramem,
- [ ] alerty błędów backupu działają,
- [ ] ostatni restore test ma wynik PASS,
- [ ] osoby z dostępem są aktualne,
- [ ] retencja jest egzekwowana,
- [ ] keystore ma sprawdzoną kopię,
- [ ] dokumentacja restore jest aktualna.
