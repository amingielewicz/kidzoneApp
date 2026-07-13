# Architecture Overview

Ostatnia aktualizacja: 2026-07-13

## Cel

Dokument opisuje wysokopoziomową architekturę kidZone i zasady utrzymania zależności między warstwami.

## Warstwy

```text
Presentation → Domain ← Data → Framework
```

Zależności powinny iść do środka, w stronę warstwy Domain.

## Presentation

Odpowiada za:

- ekrany Jetpack Compose,
- ViewModel,
- nawigację,
- `UiState` i zdarzenia jednorazowe,
- prezentację loading, empty, error, offline i permission states.

Presentation nie wykonuje bezpośrednio operacji na Firestore, Room ani Storage.

## Domain

Odpowiada za:

- modele domenowe,
- use case'y,
- interfejsy repozytoriów,
- reguły biznesowe,
- porty usług platformowych, na przykład lokalizacji, zdjęć i preferencji.

Domain nie zależy od Android SDK, Firebase ani modeli transportowych.

## Data

Odpowiada za:

- implementacje repozytoriów,
- integracje z Firestore i Storage,
- Room cache,
- Remote Config,
- synchronizację,
- mapowanie DTO, Entity i modeli domenowych.

Data decyduje o źródle danych i strategii cache.

## Framework

Obejmuje:

- Hilt,
- Firebase SDK,
- Google Maps i Google Play Services,
- WorkManager,
- Android Services i systemowe API,
- implementacje platformowych portów warstwy Domain.

## Przepływ danych

```text
Composable
  → ViewModel
  → Use Case
  → Repository Interface
  → Repository Implementation
  → Room / Firestore / Storage
  → Mapper
  → Domain Model
  → UiState
  → Composable
```

## Offline i synchronizacja

- ostatnio pobrane dane mogą być dostępne z Room,
- repository ukrywa wybór lokalnego i zdalnego źródła,
- WorkManager obsługuje operacje, które mogą poczekać,
- operacje offline mają jawny status synchronizacji,
- retry musi być idempotentne.

## Funkcje platformowe

ViewModel nie powinien zależeć od `Context`. Funkcje Androida są udostępniane przez interfejsy Domain i implementacje Data/Framework.

Dotyczy to między innymi:

- lokalizacji,
- ustawień aplikacji,
- Photo Pickera i kamery,
- powiadomień,
- aktualizacji aplikacji,
- preferencji i zasobów tekstowych.

## Obsługa błędów

Błędy techniczne są mapowane przed prezentacją:

```text
Firebase / Room / Android exception
  → Error mapper
  → Domain result
  → UiState / UiText
  → UI
```

UI nie powinno wyświetlać surowych wyjątków.

## Zasady

- jeden ekran renderuje spójny `UiState`,
- Composable nie zawiera logiki biznesowej,
- ViewModel nie zna implementacji Firebase,
- Domain nie importuje Androida ani Firebase,
- DTO i Entity nie przeciekają do UI,
- repository odpowiada za cache, limity i synchronizację,
- wspólne zachowania, takie jak obsługa uprawnień, powinny być wydzielone i współdzielone.

## Checklista

- [ ] UI nie zna Firestore, Room ani Storage,
- [ ] ViewModel korzysta z use case'a lub interfejsu repozytorium,
- [ ] Domain jest niezależny od frameworków,
- [ ] modele zewnętrzne są mapowane,
- [ ] ekran obsługuje wszystkie istotne stany,
- [ ] retry jest kontrolowane i idempotentne,
- [ ] funkcje platformowe są dostępne przez porty,
- [ ] wspólna logika nie jest duplikowana między ekranami.
