# Modules

Ostatnia aktualizacja: 2026-07-13

## Cel

Dokument opisuje aktualny logiczny podział kidZone na warstwy i pakiety oraz zasady rozwoju bez nadmiernego sprzężenia.

## Aktualny model

Projekt jest obecnie pojedynczym modułem Android z logicznym podziałem pakietów:

```text
app/
  src/main/java/com/kidzone/
    presentation/
    domain/
    data/
    di/
    navigation/
    analytics/
    messaging/
    experiment/
    logging/
    utils/
```

## presentation

Zawiera:

- ekrany Compose,
- ViewModel,
- `UiState` i eventy,
- komponenty wspólne,
- obsługę akcji użytkownika.

Nie zawiera bezpośrednich operacji Firebase, Room ani Storage.

## domain

Zawiera:

- modele domenowe,
- use case'y,
- interfejsy repozytoriów,
- porty funkcji platformowych,
- reguły biznesowe i kontrolowane wyniki.

Nie zależy od Android SDK, Compose, Firebase, Room ani Hilt.

## data

Zawiera:

- implementacje repozytoriów,
- DTO, Entity i mappery,
- DAO i Room,
- integracje Firebase,
- cache, synchronizację i retry,
- Remote Config.

## di

Odpowiada za składanie zależności przez Hilt:

- bindy interfejsów,
- provider methods,
- scope'y,
- konfigurację implementacji platformowych.

## navigation

Zawiera:

- definicje tras,
- NavGraph,
- deep linki,
- argumenty ekranów,
- zasady czyszczenia back stack po auth i account deletion.

## analytics, logging i messaging

Te pakiety powinny zawierać wyłącznie bezpieczne adaptery techniczne. Nie mogą przyjmować surowych danych użytkownika ani modeli UI.

## utils

`utils` nie może być katalogiem na kod bez właściciela. Dozwolone są małe, niezależne narzędzia, na przykład walidatory i helpery tekstowe. Logika domenowa, telemetryczna i platformowa trafia do właściwego pakietu.

## Wspólne komponenty

Kod współdzielony powinien mieć jasno określoną odpowiedzialność. Przykłady:

- `CategoryBadge` dla spójnej prezentacji kategorii,
- wspólny handler uprawnień lokalizacji i kamery,
- mappery błędów,
- komponenty formularzy i stanów loading/empty/error.

Nie kopiujemy tego samego flow między ekranami.

## Przyszła modularyzacja Gradle

Możliwy kierunek:

```text
:app
:core:domain
:core:data
:core:ui
:core:platform
:feature:auth
:feature:map
:feature:places
:feature:profile
:feature:ranking
```

Modularyzację wykonujemy dopiero przy realnym problemie z czasem buildu, ownership, zależnościami lub konfliktami w kodzie.

## Checklista PR

- [ ] nowy kod trafia do właściwego pakietu,
- [ ] klasa ma jedną odpowiedzialność,
- [ ] `utils` nie zawiera logiki biznesowej,
- [ ] Domain pozostaje niezależny,
- [ ] UI, dane i logika nie są zmieszane,
- [ ] wspólna logika nie jest duplikowana,
- [ ] telemetryczne adaptery nie przyjmują PII,
- [ ] zależność między pakietami jest zgodna z architekturą.
