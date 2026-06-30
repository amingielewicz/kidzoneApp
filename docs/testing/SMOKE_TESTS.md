# Smoke Tests

## Cel

Minimalny zestaw testów wykonywanych po każdym wdrożeniu przed udostępnieniem aplikacji użytkownikom.

## Krytyczne scenariusze

- Uruchomienie aplikacji
- Logowanie
- Rejestracja
- Mapa ładuje miejsca
- Lista miejsc
- Szczegóły miejsca
- Dodanie opinii
- Dodanie zdjęcia
- Dodanie miejsca
- Profil użytkownika
- Ranking
- Powiadomienia
- Wylogowanie

## Wynik

Każdy scenariusz:
- PASS
- FAIL
- BLOCKED

## Kryteria GO

- Brak FAIL dla scenariuszy krytycznych.
- Brak blockerów produkcyjnych.
- Release checklist zakończona.

## Checklist

- [ ] Wszystkie smoke testy PASS
- [ ] Crash free
- [ ] Backend działa
- [ ] Firebase działa
- [ ] Monitoring bez alertów