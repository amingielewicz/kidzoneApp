# Clean Architecture

## Cel

Dokument opisuje zasady Clean Architecture stosowane w KidZone.

## Główna zasada

Kod zależy od abstrakcji i reguł biznesowych, a nie od szczegółów frameworków.

```text
UI -> ViewModel -> Use Case -> Repository Interface -> Repository Implementation -> Firebase / Room / Android
```

## Warstwa Domain

Domain jest centrum aplikacji.

Może zawierać:

- modele domenowe,
- use case,
- interfejsy repozytoriów,
- interfejsy usług domenowych,
- reguły biznesowe.

Nie może zależeć od:

- Android SDK,
- Firebase SDK,
- Jetpack Compose,
- Hilt,
- Room,
- Google Maps.

## Warstwa Presentation

Presentation odpowiada za UI i stan ekranu.

Może zawierać:

- Composable,
- ViewModel,
- UI state,
- UI events,
- mapowanie danych domenowych do widoku.

Nie powinna zawierać:

- zapytań Firestore,
- bezpośredniej logiki Storage,
- decyzji biznesowych,
- dużych algorytmów domenowych.

## Warstwa Data

Data implementuje kontrakty z Domain.

Może zawierać:

- implementacje repozytoriów,
- DTO,
- mappery,
- obsługę Firebase,
- obsługę Room,
- cache i synchronizację.

## Framework

Framework to szczegóły techniczne.

Przykłady:

- FirebaseAuth,
- FirebaseFirestore,
- FirebaseStorage,
- GoogleMap,
- WorkManager,
- Android Context,
- Hilt modules.

## Reguły zależności

- Domain nie importuje frameworków.
- Presentation zna Domain.
- Data zna Domain i Framework.
- Framework nie przecieka do Domain.
- DTO nie przeciekają do UI.

## Antywzorce

- ViewModel z `Context`.
- Composable wykonujący request do repozytorium.
- Model domenowy zależny od Firestore annotation.
- Firebase exception pokazywany użytkownikowi bez mapowania.
- Jeden ViewModel obsługujący kilka niezależnych feature'ów.

## Checklist PR

- [ ] Domain bez importów Android/Firebase.
- [ ] UI nie wykonuje requestów bezpośrednio.
- [ ] ViewModel używa abstrakcji.
- [ ] Błędy są mapowane.
- [ ] Dane zewnętrzne są mapowane na modele domenowe.
