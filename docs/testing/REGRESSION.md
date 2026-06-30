# Regression Testing

## Cel

Dokument definiuje zakres testów regresyjnych wykonywanych przed każdym wydaniem oraz po zmianach wpływających na istniejącą funkcjonalność.

## Kiedy wykonywać

- przed każdym releasem,
- po poprawkach błędów krytycznych,
- po refaktoryzacji,
- po zmianach Firebase,
- po zmianach architektury,
- po aktualizacji bibliotek.

## Zakres regresji

### Authentication
- logowanie
- rejestracja
- wylogowanie
- reset hasła

### Places
- dodawanie
- edycja
- usuwanie
- wyszukiwanie
- filtrowanie

### Reviews
- dodawanie
- edycja
- usuwanie
- zgłaszanie

### Maps
- ładowanie mapy
- markery
- klastry
- geolokalizacja

### User
- profil
- odznaki
- ranking
- ustawienia

### Backend
- Cloud Functions
- FCM
- Firestore Rules
- Storage Rules

## Kryteria zakończenia

- brak regresji w funkcjach krytycznych,
- brak błędów Blocker i Critical,
- wszystkie smoke testy zakończone powodzeniem,
- zaakceptowana decyzja GO.

## Checklist

- [ ] Wszystkie scenariusze wykonane.
- [ ] Wyniki udokumentowane.
- [ ] Nowe błędy zgłoszone.
- [ ] Poprawki zweryfikowane.
- [ ] Release może zostać zatwierdzony.