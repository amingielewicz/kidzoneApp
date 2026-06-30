# Architecture Handbook

## Cel

Ten dokument opisuje docelową architekturę aplikacji KidZone i zasady utrzymania jej w ryzach podczas rozwoju.

## Warstwy

```text
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
