# Dependency Rules

Ostatnia aktualizacja: 2026-07-14

## Cel

Reguły zależności między warstwami i pakietami kidZone. Ich celem jest ograniczenie sprzężenia, wycieków frameworków i mieszania odpowiedzialności.

## Główna zasada

Zależności kierują się do wewnątrz architektury:

```text
Presentation → Domain ← Data → Framework
                 ↑
                 DI składa implementacje
```

Domain pozostaje niezależny od Android SDK, Compose, Firebase, Room, Hilt i Navigation.

## Dozwolone zależności

- Presentation → Domain,
- Presentation → Navigation abstractions,
- Data → Domain,
- Data → Firebase, Room i inne frameworki techniczne,
- Framework adapters → Domain ports,
- DI → wszystkie warstwy wyłącznie do składania zależności,
- testy → kod produkcyjny i test doubles.

## Niedozwolone zależności

- Domain → Android SDK,
- Domain → Firebase,
- Domain → Room,
- Domain → Compose,
- Domain → Hilt,
- Presentation → Firebase SDK,
- Presentation → DAO,
- Presentation → konkretna implementacja repository,
- Composable → Repository,
- ViewModel → `NavController`, `Activity`, `View` lub `Context`,
- Data → Composable albo ekran,
- wspólny moduł/core → konkretny feature bez uzasadnienia.

## Komunikacja między warstwami

```text
Composable
  → ViewModel action
  → Use Case / Repository interface
  → Repository implementation
  → Firebase / Room / platform adapter
```

W drugą stronę dane wracają przez:

- modele domenowe,
- kontrolowane wyniki,
- `Flow` lub `StateFlow`,
- mapowane błędy.

DTO, Entity, `DocumentSnapshot`, `FirebaseException`, `Cursor` i typy frameworkowe nie powinny docierać do UI.

## Porty platformowe

Funkcje wymagające Androida lub usług systemowych są ukrywane za interfejsem, na przykład:

- lokalizacja,
- stan sieci,
- zegar,
- dispatcher,
- logger,
- analytics,
- otwieranie ustawień,
- odczyt wersji aplikacji.

Implementacja może używać Android SDK, ale kontrakt pozostaje neutralny.

## Navigation

- ViewModel emituje event lub wynik,
- ekran wykonuje nawigację,
- `NavController` pozostaje w warstwie UI,
- route przekazuje małe argumenty,
- pełne obiekty są ponownie pobierane po identyfikatorze.

## Logging i telemetryka

Warstwy wyższe nie zależą bezpośrednio od Crashlytics ani Analytics SDK. Korzystają z ograniczonej fasady, która:

- nie przyjmuje surowych modeli użytkownika,
- nie loguje PII,
- ma stabilne nazwy eventów,
- może zostać zastąpiona w testach.

## DI

DI może znać implementacje, ale:

- logika nie trafia do modułów Hilt,
- provider nie wykonuje operacji sieciowych,
- scope odpowiada lifecycle,
- interfejs z Domain jest wiązany z implementacją Data/Framework,
- zależność cykliczna jest błędem architektury.

## Wyjątki

Każdy wyjątek od reguły wymaga:

- uzasadnienia w PR,
- opisu ryzyka,
- planu usunięcia albo świadomej decyzji architektonicznej,
- testów ograniczających regresję.

Wyjątek tymczasowy nie może stać się domyślnym wzorcem.

## Kontrola jakości

Podczas review sprawdź:

- kierunek importów,
- brak typów frameworkowych w Domain,
- brak DTO/Entity w UI,
- brak logiki biznesowej w Composable,
- brak bezpośrednich wywołań Firebase w Presentation,
- brak `Context` w ViewModelu,
- brak cykli zależności,
- brak przenoszenia kodu do `utils` tylko po to, aby ominąć warstwy.

## Checklista

- [ ] Domain jest niezależny,
- [ ] UI korzysta z abstrakcji,
- [ ] Data implementuje porty Domain,
- [ ] framework nie przecieka do Presentation,
- [ ] ViewModel nie zna nawigacji ani Android Context,
- [ ] mappery oddzielają modele techniczne,
- [ ] telemetryka jest za fasadą,
- [ ] nowa klasa trafiła do właściwej warstwy,
- [ ] wyjątki są jawnie udokumentowane.
