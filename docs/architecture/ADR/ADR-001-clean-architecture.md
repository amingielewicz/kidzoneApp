# ADR-001: Use Clean Architecture

Ostatnia aktualizacja: 2026-07-14

## Status

Accepted

## Context

kidZone łączy Compose UI, logikę domenową, Firebase, Room, WorkManager i usługi Androida. Bez wyraźnych granic frameworki zaczynają przeciekać do logiki biznesowej, a testy i migracje stają się trudniejsze.

## Decision

Projekt stosuje logiczne warstwy:

```text
Presentation → Domain ← Data → Framework
```

Domain pozostaje najbardziej stabilną warstwą i nie zależy od Android SDK, Compose, Firebase, Room, Hilt ani Navigation.

## Rules

- UI komunikuje się z ViewModel,
- ViewModel używa use case'ów lub interfejsów repository,
- Data implementuje kontrakty Domain,
- frameworki są ukryte za adapterami,
- DTO, Entity i wyjątki SDK nie trafiają do UI,
- nawigacja pozostaje w Presentation,
- DI wyłącznie składa zależności,
- telemetryka jest dostępna przez ograniczoną fasadę.

## Consequences

### Positive

- prostsze testy jednostkowe,
- kontrolowany kierunek zależności,
- łatwiejsza wymiana źródeł danych,
- mniejsze ryzyko przecieku Firebase lub Androida,
- lepsza obsługa cache, offline i migracji.

### Trade-offs

- więcej interfejsów i mapperów,
- większa liczba modeli,
- konieczność dyscypliny podczas review,
- ryzyko nadmiernej abstrakcji przy prostych funkcjach.

## Rejected alternatives

### Bezpośredni dostęp do Firebase z ViewModeli

Odrzucony z powodu sprzężenia z SDK, utrudnionych testów i niespójnego error handling.

### Jeden wspólny model dla Firestore, Room i UI

Odrzucony z powodu przecieku wymagań technicznych między warstwami oraz trudniejszych migracji.

## Compliance

Nowe zależności powinny być sprawdzane według `docs/architecture/DEPENDENCY_RULES.md`. Wyjątek wymaga jawnego uzasadnienia w PR.
