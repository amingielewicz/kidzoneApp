# Dependency Rules

## Cel

Dokument definiuje reguły zależności pomiędzy warstwami i pakietami w projekcie KidZone.

## Główna zasada

Zależności zawsze kierują się do wewnątrz architektury.

```text
Presentation
      ↓
Domain
      ↑
Data
      ↓
Framework
```

## Dozwolone zależności

- Presentation → Domain
- Presentation → Navigation
- Data → Domain
- Data → Framework
- DI → wszystkie warstwy wyłącznie w celu składania zależności.

## Niedozwolone zależności

- Domain → Android
- Domain → Firebase
- Domain → Room
- Domain → Compose
- Presentation → Firebase SDK
- Presentation → DAO
- Composable → Repository
- ViewModel → NavController

## Komunikacja między warstwami

- UI komunikuje się z ViewModel.
- ViewModel wywołuje Use Case lub Repository Interface.
- Repository Implementation komunikuje się z Firebase lub Room.
- Mappery tłumaczą modele zewnętrzne na modele domenowe.

## Kontrola jakości

Podczas code review należy sprawdzić:

- kierunek importów,
- brak przecieków DTO do UI,
- brak frameworków w Domain,
- brak logiki biznesowej w Composable,
- brak wywołań Firebase w UI.

## Checklist

- [ ] Importy zgodne z architekturą.
- [ ] Domain jest niezależny.
- [ ] UI korzysta wyłącznie z abstrakcji.
- [ ] Framework nie przecieka do Presentation.
- [ ] Nowe klasy trafiły do właściwej warstwy.