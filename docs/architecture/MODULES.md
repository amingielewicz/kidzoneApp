# Modules

## Cel

Dokument opisuje logiczny podział projektu KidZone na moduły i pakiety oraz odpowiedzialności każdego obszaru kodu.

## Aktualny model

Projekt Android jest obecnie utrzymywany jako aplikacja z logicznym podziałem pakietów.

```text
app/
  src/main/java/com/kidzone/
    presentation/
    domain/
    data/
    di/
    navigation/
    utils/
    analytics/
    messaging/
    experiment/
    logging/
```

## presentation

Odpowiada za UI.

Zawiera:

- ekrany Compose,
- ViewModele,
- UI state,
- komponenty wspólne,
- obsługę akcji użytkownika.

Nie powinno zawierać:

- bezpośrednich wywołań Firebase,
- logiki Storage,
- zapytań SQL/Room,
- ciężkiej logiki domenowej.

## domain

Odpowiada za reguły biznesowe.

Zawiera:

- modele domenowe,
- interfejsy repozytoriów,
- use case,
- interfejsy usług domenowych.

Nie powinno zależeć od:

- Android SDK,
- Compose,
- Firebase,
- Room,
- Hilt.

## data

Odpowiada za dane i integracje.

Zawiera:

- implementacje repozytoriów,
- DTO,
- encje Room,
- DAO,
- mappery,
- Remote Config,
- Firebase integrations.

## di

Odpowiada za Hilt i składanie zależności.

Zawiera:

- moduły Hilt,
- bindy interfejsów,
- provider methods.

## navigation

Odpowiada za routing aplikacji.

Zawiera:

- definicje tras,
- NavGraph,
- deep linki,
- argumenty ekranów.

## utils

Zawiera pomocnicze narzędzia techniczne.

Dozwolone:

- mapowanie błędów,
- helpery tekstowe,
- walidatory,
- utility niezależne od UI.

Nie powinno stać się śmietnikiem. Jeśli klasa ma właściciela domenowego, powinna trafić do właściwego pakietu feature albo warstwy.

## Możliwy przyszły podział na moduły Gradle

Docelowo projekt można rozdzielić na moduły:

```text
:app
:core:domain
:core:data
:core:ui
:feature:auth
:feature:map
:feature:places
:feature:profile
:feature:ranking
```

Taki podział warto robić dopiero wtedy, gdy projekt zacznie realnie cierpieć przez czas buildu, konflikty w kodzie albo zbyt szerokie zależności.

## Checklist PR

- [ ] Nowy kod trafia do właściwego pakietu.
- [ ] Klasa ma jedną odpowiedzialność.
- [ ] `utils` nie dostaje logiki biznesowej.
- [ ] `domain` pozostaje niezależny od frameworków.
- [ ] Feature nie miesza UI, danych i logiki biznesowej w jednej klasie.
