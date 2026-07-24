# Clean Architecture

Ostatnia aktualizacja: 2026-07-13

## Cel

Zasady Clean Architecture stosowane w kidZone. Kod zależy od abstrakcji i reguł biznesowych, a nie od szczegółów Androida, Firebase czy UI.

## Kierunek zależności

```text
UI → ViewModel → Use Case → Repository Interface
                              ↑
                    Repository Implementation → Firebase / Room / Android
```

Warstwa Domain pozostaje centrum aplikacji.

## Domain

Może zawierać:

- modele domenowe,
- use case'y,
- interfejsy repozytoriów,
- interfejsy usług platformowych,
- reguły biznesowe,
- kontrolowane typy wyników i błędów.

Nie zależy od:

- Android SDK,
- Firebase SDK,
- Jetpack Compose,
- Hilt,
- Room,
- Google Maps.

## Presentation

Zawiera:

- Composable,
- ViewModel,
- `UiState`,
- zdarzenia UI,
- mapowanie modeli domenowych na dane widoku.

Nie zawiera:

- zapytań Firestore,
- operacji Storage,
- decyzji o cache,
- surowych wyjątków technicznych,
- logiki biznesowej możliwej do przetestowania poza UI.

## Data

Implementuje kontrakty Domain i zawiera:

- repozytoria,
- DTO i Entity,
- mappery,
- integracje Firebase i Room,
- synchronizację,
- cache,
- retry i idempotency.

## Framework

Obejmuje szczegóły techniczne:

- Firebase Auth, Firestore i Storage,
- Google Maps,
- WorkManager,
- Android `Context`,
- systemowe uprawnienia,
- Hilt modules.

Framework implementuje porty zdefiniowane w Domain i nie przecieka do środka.

## Funkcje platformowe

ViewModel nie powinien zależeć bezpośrednio od `Context`. Operacje takie jak:

- lokalizacja,
- otwieranie ustawień aplikacji,
- kamera i Photo Picker,
- powiadomienia,
- zasoby tekstowe,
- preferencje,

powinny być dostępne przez interfejsy lub wydzielone kontrolery platformowe.

## Reguły zależności

- Domain nie importuje frameworków.
- Presentation zna Domain.
- Data implementuje kontrakty Domain.
- DTO nie przeciekają do UI.
- surowe wyjątki są mapowane,
- wspólna logika nie jest kopiowana między ekranami,
- repozytorium ukrywa źródło danych.

## Antywzorce

- ViewModel z `Context`,
- Composable wywołujący Firebase,
- model domenowy z adnotacją Firestore,
- wyjątek Firebase pokazany użytkownikowi,
- ten sam flow uprawnień skopiowany w kilku ekranach,
- jeden ViewModel obsługujący niezależne funkcje,
- DTO lub `DocumentSnapshot` zwracany do UI.

## Checklista PR

- [ ] Domain bez importów Android i Firebase,
- [ ] UI nie wykonuje requestów bezpośrednio,
- [ ] ViewModel używa abstrakcji,
- [ ] błędy są mapowane,
- [ ] modele zewnętrzne są mapowane,
- [ ] funkcje platformowe są odseparowane,
- [ ] logika współdzielona nie jest duplikowana,
- [ ] retry i operacje zapisu są idempotentne.
