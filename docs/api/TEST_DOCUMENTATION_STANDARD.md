# Standard Dokumentacji dla Testów (Unit & Integration)

Testy w projekcie kidZone muszą posiadać jasny opis celu, użytych technik (mockowanie) oraz sprawdzanych scenariuszy.

## Szablon (Template)

```kotlin
/**
 * 🧪 Cel testu:
 * - [Opis co jest testowane, np. logika biznesowa AddPlaceViewModel]
 *
 * 🛠️ Środowisko:
 * - [Użyte narzędzia: MockK, MainDispatcherRule, SavedStateHandle]
 * - [Strategia: odizolowane testy jednostkowe z mockami repozytoriów]
 *
 * 🔍 Scenariusze:
 * - [Główne grupy testów, np. walidacja formularza, proces zapisu, obsługa błędów]
 */
```

## Zasady pisania
1. **Wyrazistość**: Unikaj opisów technicznych typu "testuje funkcję x". Opisz *zachowanie* systemu.
2. **Setup**: Jasno określ, jakie warunki brzegowe są przygotowywane w sekcji `@BeforeEach`.
3. **Zwięzłość**: Dokumentacja testu powinna być "mapą drogową" dla czytającego kod.
