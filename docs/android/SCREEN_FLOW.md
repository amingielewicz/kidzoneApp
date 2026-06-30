# Android Screen Flow

## Cel

Dokument opisuje przepływ użytkownika przez aplikację KidZone oraz zależności pomiędzy ekranami.

## Główny przepływ

```text
Splash
 ├─ Onboarding (pierwsze uruchomienie)
 ├─ Logowanie / Rejestracja
 └─ Home
      ├─ Mapa
      │    └─ Szczegóły miejsca
      │          ├─ Opinie
      │          ├─ Zdjęcia
      │          └─ Dodaj opinię
      ├─ Lista miejsc
      │    └─ Szczegóły miejsca
      ├─ Ranking
      ├─ Profil
      │    ├─ Moje miejsca
      │    ├─ Moje opinie
      │    ├─ Odznaki
      │    └─ Ustawienia
      └─ Dodaj miejsce
```

## Zasady nawigacji

- Każdy ekran powinien mieć jasno określony cel.
- Liczba kroków do wykonania głównej akcji powinna być minimalna.
- Back stack powinien być przewidywalny.
- Deep link powinien otwierać właściwy ekran.

## Stany ekranów

Każdy ekran powinien obsługiwać:

- Loading
- Empty
- Success
- Error
- Offline (jeżeli dotyczy)

## Checklist

- [ ] Wszystkie ekrany mają loading state.
- [ ] Wszystkie listy mają empty state.
- [ ] Wszystkie błędy mają czytelny komunikat.
- [ ] Deep linki prowadzą do poprawnych ekranów.
- [ ] Nawigacja jest spójna w całej aplikacji.
