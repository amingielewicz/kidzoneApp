# Coding Guidelines

## Cel

Zasady kodowania dla KidZone mają utrzymać projekt czytelny, testowalny i łatwy do rozwijania.

## Architektura

Projekt utrzymuje podział:

```text
Presentation -> Domain -> Data -> Framework
```

Zasady:

- ViewModel nie importuje `android.content.Context`.
- Logika biznesowa nie siedzi w Composable.
- Composable powinien być możliwie bezstanowy.
- Repository jest jednym źródłem prawdy dla danych.
- Use Case opisuje jedną operację biznesową.

## Kotlin

- Używamy czytelnych nazw, nie skrótów.
- Preferujemy `val` nad `var`.
- Unikamy nullable bez potrzeby.
- Obsługujemy błędy jawnie.
- Nie połykamy wyjątków pustym `catch`.
- Publiczne API powinno mieć KDoc, jeśli nie jest oczywiste.

## Compose

- Ciężka logika poza Composable.
- Listy przez `LazyColumn` / `LazyRow`.
- Elementy list mają stabilny `key`.
- Stan ekranu trzymamy w ViewModelu.
- UI renderuje `UiState`.
- Efekty uboczne przez `LaunchedEffect`, `DisposableEffect` lub ViewModel.

## ViewModel

ViewModel powinien:

- udostępniać `StateFlow<UiState>`,
- obsługiwać akcje użytkownika,
- korzystać z Use Case / Repository,
- nie znać szczegółów Android framework,
- nie zawierać kodu UI.

## Repository

Repository powinno:

- ukrywać szczegóły Firestore / Room,
- obsługiwać cache,
- ograniczać liczbę requestów,
- stosować paginację i limity,
- zwracać stabilne modele domenowe.

## Logging

- Nie logujemy danych uwierzytelniających.
- Nie logujemy e-maili.
- Nie logujemy surowej lokalizacji użytkownika.
- Nie logujemy pełnych payloadów formularzy.
- Release logging musi być ograniczony i bez danych wrażliwych.

## PR checklist

- [ ] Kod jest czytelny.
- [ ] Brak ciężkiej logiki w Composable.
- [ ] Błędy są obsłużone.
- [ ] Dodano lub zaktualizowano testy, jeśli trzeba.
- [ ] Brak danych wrażliwych w logach.
- [ ] Build przechodzi.
