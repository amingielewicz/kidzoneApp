<<<<<<< HEAD
# Architecture Handbook

## Cel

Ten dokument opisuje docelową architekturę aplikacji KidZone i zasady utrzymania jej w ryzach podczas rozwoju.
=======
# Architecture Overview

## Cel

Dokument opisuje wysokopoziomową architekturę KidZone oraz główne zasady projektowania aplikacji.
>>>>>>> 7f3496a (docs: add architecture overview)

## Warstwy

```text
<<<<<<< HEAD
Presentation -> Domain -> Data -> Framework
```

### Presentation

- Compose Screens.
- ViewModels.
- Navigation.
- UI state.
- Obsługa akcji użytkownika.

Presentation nie powinna znać szczegółów Firestore, Room ani Google Maps poza komponentami UI.

### Domain

- Modele domenowe.
- Interfejsy repository.
- Use case.
- Abstrakcje usług, np. lokalizacja, kompresja zdjęć, preferencje.

Domain nie powinien zależeć od Android framework.

### Data

- Implementacje repository.
- Firestore.
- Room cache.
- Remote Config.
- Storage.
- Mapowanie DTO / Entity / Domain.

Data odpowiada za pobieranie, cache i synchronizację.

### Framework

- Hilt.
- Firebase SDK.
- Google Play Services.
- Android services.

## Przepływ danych

```text
Composable
  -> ViewModel
  -> UseCase
  -> Repository
  -> Room / Firestore / Storage
```

## Zasady

- Jeden ekran renderuje jeden spójny `UiState`.
- ViewModel nie wykonuje bezpośrednio zapytań do Firebase SDK.
- Repository jest jedynym miejscem, które decyduje o cache i źródle danych.
- Firestore i Room nie powinny przeciekać do warstwy UI.
- Modele domenowe nie powinny być zależne od DTO backendu.

## Offline i cache

- Ostatnio pobrane miejsca powinny być dostępne lokalnie.
- Operacje offline powinny mieć status synchronizacji.
- WorkManager powinien obsługiwać retry tam, gdzie operacja może poczekać.

## Błędy

Każdy flow danych musi mieć obsługę:

- loading,
- success,
- empty,
- error,
- offline.

## Checklist architektoniczny

- [ ] UI nie zna szczegółów Firestore.
- [ ] ViewModel korzysta z Use Case / Repository.
- [ ] Repository obsługuje cache i limity.
- [ ] Modele domenowe są oddzielone od DTO.
- [ ] Ekran ma kompletny UiState.
- [ ] Błędy są obsłużone jawnie.
=======
Presentation
Domain
Data
Framework
```

## Presentation

Odpowiada za:

- ekrany Compose,
- ViewModel,
- UI state,
- obsługę akcji użytkownika,
- prezentację błędów i stanów ładowania.

Presentation nie powinna zawierać logiki biznesowej.

## Domain

Odpowiada za:

- modele domenowe,
- use case,
- interfejsy repozytoriów,
- reguły biznesowe niezależne od Firebase i Androida.

Domain nie zależy od frameworków.

## Data

Odpowiada za:

- implementacje repozytoriów,
- integracje z Firebase,
- Room cache,
- mapowanie DTO na modele domenowe,
- synchronizację danych.

## Framework

Odpowiada za:

- Firebase SDK,
- Google Maps,
- Android Services,
- Hilt,
- WorkManager,
- platformowe API Androida.

## Kierunek zależności

```text
Presentation -> Domain <- Data -> Framework
```

Zależności powinny iść do środka, czyli w stronę Domain.

## Zasady

- Domain pozostaje niezależny.
- ViewModel nie zna implementacji Firebase.
- Composable nie wykonuje zapytań do repozytoriów.
- Data mapuje modele zewnętrzne na modele domenowe.
- Framework jest szczegółem implementacyjnym.

## Checklist

- [ ] Logika biznesowa jest poza UI.
- [ ] Domain nie importuje Android/Firebase.
- [ ] ViewModel korzysta z use case albo repozytorium przez interfejs.
- [ ] DTO nie przeciekają do UI.
- [ ] Błędy są mapowane na stan UI.
>>>>>>> 7f3496a (docs: add architecture overview)
