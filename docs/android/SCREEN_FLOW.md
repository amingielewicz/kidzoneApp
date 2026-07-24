# Android Screen Flow

Ostatnia aktualizacja: 2026-07-13

## Cel

Dokument opisuje główne przepływy użytkownika w kidZone oraz zależności między ekranami.

## Główny przepływ

```text
Splash
 ├─ Onboarding (pierwsze uruchomienie)
 ├─ Logowanie / Rejestracja
 └─ Główna część aplikacji
      ├─ Start
      │    └─ Szczegóły miejsca
      ├─ Mapa
      │    └─ Szczegóły miejsca
      ├─ Lista miejsc
      │    └─ Szczegóły miejsca
      ├─ Ranking
      ├─ Profil
      │    ├─ Moje miejsca
      │    ├─ Moje opinie
      │    ├─ Odznaki
      │    ├─ Konto i bezpieczeństwo
      │    └─ Dokumenty prawne
      └─ Dodaj miejsce
```

## Szczegóły miejsca

```text
Szczegóły miejsca
 ├─ Galeria zdjęć
 ├─ Opinie i oceny
 ├─ Dodaj opinię
 ├─ Dodaj zdjęcie
 ├─ Zgłoś treść
 ├─ Zaproponuj poprawkę
 └─ Otwórz nawigację
```

## Konto

```text
Profil
 └─ Konto i bezpieczeństwo
      ├─ Zmiana danych
      ├─ Zmiana lub reset hasła
      ├─ Wylogowanie
      └─ Usunięcie konta
```

Po wylogowaniu, ban sign-out albo usunięciu konta użytkownik nie może wrócić przyciskiem Back do części zalogowanej.

## Uprawnienia

Punkty wejścia do lokalizacji:

- Start — akcja włączenia lokalizacji,
- Mapa — „Moja lokalizacja”,
- Lista — sortowanie od najbliższych,
- formularz miejsca — pobranie lokalizacji.

Punkty wejścia do kamery i zdjęć:

- avatar,
- dodawanie miejsca,
- opinia,
- szczegóły miejsca.

Photo Picker działa bez szerokiego dostępu do galerii. Trwała odmowa lokalizacji lub kamery prowadzi do ustawień aplikacji.

## Deep linki i powiadomienia

Deep link może otworzyć konkretny zasób, ale musi:

- zweryfikować argument,
- obsłużyć brak zasobu,
- respektować stan logowania i blokady konta,
- nie tworzyć nieprawidłowego back stacku.

## Stany ekranów

Każdy ekran obsługuje odpowiednie stany:

- loading,
- success,
- empty,
- error,
- offline,
- permission required,
- service disabled,
- saving lub submitting.

## Zasady UX

- główna akcja ma możliwie krótki flow,
- Back jest przewidywalny,
- błąd nie usuwa bez potrzeby danych formularza,
- retry nie tworzy duplikatów,
- brak lokalizacji, kamery lub powiadomień nie blokuje całej aplikacji,
- lista stanowi fallback dla mapy.

## Checklista

- [ ] wszystkie główne ekrany są osiągalne,
- [ ] loading, empty, error i offline są obsłużone,
- [ ] flow auth czyści back stack,
- [ ] account deletion kończy sesję,
- [ ] deep linki prowadzą do właściwych ekranów,
- [ ] odmowy uprawnień nie tworzą martwych akcji,
- [ ] formularze są odporne na double submit,
- [ ] nawigacja jest spójna między Startem, Mapą i Listą.
