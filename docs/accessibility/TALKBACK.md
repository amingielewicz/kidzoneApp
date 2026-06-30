# TalkBack Guide

## Cel

Standard testowania aplikacji z użyciem Android TalkBack.

## Wymagania

- Każdy element interaktywny posiada contentDescription lub semantykę Compose.
- Kolejność fokusu jest logiczna.
- Elementy dekoracyjne nie są odczytywane.
- Komunikaty dynamiczne są ogłaszane użytkownikowi.

## Scenariusze testowe

- Uruchomienie aplikacji.
- Logowanie.
- Nawigacja po ekranie głównym.
- Lista miejsc.
- Szczegóły miejsca.
- Dodanie opinii.
- Formularze.
- Dialogi.

## Checklist

- [ ] Focus order poprawny.
- [ ] Wszystkie przyciski mają etykiety.
- [ ] Brak pustych elementów odczytywanych przez TalkBack.
- [ ] Dynamiczne komunikaty są odczytywane.