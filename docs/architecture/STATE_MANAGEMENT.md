# State Management

Ostatnia aktualizacja: 2026-07-13

## Cel

Zasady zarządzania stanem w kidZone z jednym źródłem prawdy dla ekranu i jednoznacznym rozdzieleniem trwałego stanu od zdarzeń jednorazowych.

## Przepływ

```text
Repository / Use Case
        ↓
ViewModel
        ↓
StateFlow<UiState>
        ↓
Compose UI
```

## UiState

Większy ekran powinien mieć jawny, niemutowalny `UiState`.

Typowe elementy:

- dane,
- loading i refreshing,
- saving lub submitting,
- empty state,
- błąd,
- filtry i sortowanie,
- stan offline,
- stan wymaganej zgody lub usługi systemowej.

Dla złożonych ekranów preferowany jest sealed state albo spójna data class zamiast kilku niezależnych flag.

## StateFlow

`StateFlow` przechowuje trwały stan:

- listę miejsc,
- dane formularza,
- wybrane filtry,
- aktualnego użytkownika,
- status synchronizacji,
- loading, error i offline state.

State powinien mieć sens po ponownej subskrypcji UI.

## Eventy jednorazowe

Jednorazowe akcje nie należą do trwałego stanu:

- snackbar,
- nawigacja po sukcesie,
- otwarcie ustawień aplikacji,
- uruchomienie systemowego pickera,
- jednorazowy dialog,
- komunikat po zapisie.

Można użyć `SharedFlow`, kanału eventów lub jawnego callbacku platformowego. Event nie może odtwarzać się przypadkowo po rotacji lub powrocie do ekranu.

## Compose

Composable:

- obserwuje stan z lifecycle awareness,
- renderuje UI,
- wysyła intencje użytkownika,
- przechowuje wyłącznie lokalny stan prezentacyjny,
- nie wykonuje logiki biznesowej ani requestów do danych.

Lokalny state może obejmować scroll, rozwinięcie menu, widoczność sheet/dialog i roboczy tekst pola, jeśli ViewModel nie musi go odtwarzać.

## ViewModel

ViewModel:

- agreguje dane,
- obsługuje akcje,
- mapuje wyniki i błędy,
- aktualizuje stan atomowo przez `copy`,
- nie przechowuje `Activity`, `View` ani `Context`,
- nie uruchamia systemowych dialogów bezpośrednio.

## Uprawnienia i powrót z ustawień

Stan zgody należy odświeżyć po powrocie aplikacji do foreground. Samo jednorazowe wysłanie eventu „otwórz ustawienia” nie może być źródłem prawdy.

Flow powinien rozróżniać:

- brak zgody,
- zwykłą odmowę,
- trwałą odmowę,
- zgodę nadaną,
- wyłączoną usługę systemową.

## Formularze

- walidacja pól jest częścią stanu,
- podwójne wysłanie jest blokowane,
- stan zapisu jest jawny,
- sukces nawigacyjny jest eventem,
- dane formularza nie znikają przy chwilowym błędzie,
- retry nie tworzy duplikatów.

## Antywzorce

- wiele źródeł prawdy,
- snackbar jako trwały state,
- mutowanie listy bez `copy`,
- `Context` w ViewModelu,
- logika biznesowa w Composable,
- systemowy dialog uruchamiany ponownie po każdej recomposition,
- event zapisany jako boolean bez mechanizmu konsumpcji.

## Checklista

- [ ] ekran ma jedno źródło prawdy,
- [ ] `UiState` jest jawny i niemutowalny,
- [ ] eventy nie są trwałym stanem,
- [ ] UI tylko renderuje i wysyła akcje,
- [ ] ViewModel nie zależy od `Context`,
- [ ] powrót z ustawień odświeża realny stan,
- [ ] formularze są odporne na double submit,
- [ ] retry jest bezpieczne.
