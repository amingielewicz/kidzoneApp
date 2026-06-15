# Milestone 1: Data & scalability foundation

## Cel

Ustabilizować fundament pobierania danych i przygotować aplikację KidZone na większą liczbę miejsc, opinii oraz użytkowników.

Ten milestone skupia się na tym, żeby aplikacja nie pobierała niepotrzebnie całej bazy, nie dublowała requestów między ekranami i dawała się realnie przetestować na większych danych.

## Powiązane issue

- #169 — `perf: optimize places data loading foundation`
- #179 — `data: large test dataset and scalability validation`

## Sugerowany branch roboczy

```text
perf/data-scalability-foundation
```

## Zakres funkcjonalny

### Data loading

- [ ] Sprawdzić, czy ekrany Start / Mapa / Lista / Ranking nie pobierają osobno całej kolekcji miejsc.
- [ ] Uporządkować jedno źródło prawdy dla miejsc, np. `PlacesRepository`.
- [ ] Dodać lub dopracować cache miejsc w pamięci i/lub przez Room.
- [ ] Dodać limity zapytań dla głównych ekranów.
- [ ] Dodać paginację listy miejsc.
- [ ] Dodać debounce wyszukiwarki.
- [ ] Przenieść ciężkie filtrowanie i sortowanie poza Composable.
- [ ] Upewnić się, że listy Compose używają stabilnych kluczy, np. `key = place.id`.

### Ranking i statystyki

- [ ] Ranking powinien korzystać z gotowych pól, np. `ratingAverage`, `reviewsCount`, `rankingScore`.
- [ ] Karty miejsc nie powinny pobierać opinii osobnymi requestami.
- [ ] Profil użytkownika nie powinien liczyć statystyk przez skanowanie całej bazy.

### Dane testowe

- [ ] Przygotować powtarzalny generator lub seed danych.
- [ ] Przygotować dataset minimum 100 miejsc.
- [ ] Przygotować dataset minimum 1000 miejsc.
- [ ] Dodać przykładowe opinie i oceny.
- [ ] Dodać przykładowych użytkowników.
- [ ] Udokumentować sposób uruchomienia danych testowych.

## Proponowane limity startowe

| Obszar | Limit startowy |
|---|---:|
| Start — miejsca blisko | 10 |
| Start — TOP blisko | 10 |
| Lista miejsc | 20 na stronę |
| Ranking | TOP 50 / TOP 100 |
| Mapa | 100–200 markerów na widoczny obszar |
| Wyszukiwarka | debounce 300–500 ms |

## Kryteria akceptacji

- [ ] Szybkie przełączanie zakładek nie powoduje pełnego pobierania miejsc za każdym razem.
- [ ] Lista miejsc ma limit i/lub paginację.
- [ ] Wyszukiwarka nie uruchamia ciężkiej logiki przy każdej literze bez debounce.
- [ ] Ranking nie liczy średnich ocen z opinii przy każdym wejściu na ekran.
- [ ] Karty miejsc nie wykonują dodatkowych zapytań po opinie.
- [ ] Aplikacja działa na danych testowych 100 miejsc.
- [ ] Aplikacja działa na danych testowych 1000 miejsc.
- [ ] Wyniki testów skalowalności są udokumentowane.

## Manual test checklist

- [ ] Uruchomić aplikację na pustej bazie.
- [ ] Uruchomić aplikację na 100 miejscach.
- [ ] Uruchomić aplikację na 1000 miejscach.
- [ ] Sprawdzić Start.
- [ ] Sprawdzić Listę.
- [ ] Sprawdzić Ranking.
- [ ] Sprawdzić Profil.
- [ ] Szybko przełączać dolną nawigację.
- [ ] Szybko wpisywać tekst w wyszukiwarce.
- [ ] Zweryfikować liczbę requestów / odczytów tam, gdzie to możliwe.

## Ryzyka

- Zmiana kolejności sortowania listy miejsc.
- Brakujące indeksy Firestore po dodaniu nowych zapytań.
- Problemy z cache po aktualizacji miejsca/opinii.
- Różnice między danymi online i offline.
- Regresje w rankingach, jeśli obecnie są liczone dynamicznie.

## Kolejny milestone

Po zamknięciu tego milestone przechodzimy do:

- #171 — `perf: optimize map markers and bounds loading`
