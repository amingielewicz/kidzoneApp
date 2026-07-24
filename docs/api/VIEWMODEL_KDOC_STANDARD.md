# Standard Dokumentacji KDoc dla ViewModeli

Każdy ViewModel w projekcie kidZone musi posiadać ustrukturyzowaną dokumentację KDoc umieszczoną nad deklaracją klasy. Standard ten zapewnia spójność architektoniczną i ułatwia onboarding.

## Szablon (Template)

```kotlin
/**
 * 🎯 Odpowiedzialności:
 * - [Opis głównych zadań klasy]
 *
 * 🚫 Poza zakresem:
 * - [Czego ta klasa NIE robi, np. brak decyzji o offline queue, brak zarządzania sesją]
 *
 * 📥 Wejście:
 * - [Źródła danych: repozytoria, SavedStateHandle, interakcje użytkownika]
 *
 * 📤 Wyjście:
 * - [Stan UI (UiState), zdarzenia jednorazowe, nawigacja]
 *
 * ✅ Gwarancje:
 * - [Obietnice logiczne, np. deterministyczny stan, brak wycieków danych]
 *
 * 🔌 Offline:
 * - [Jak klasa zachowuje się bez sieci, wsparcie dla cache Room]
 *
 * 🧵 Wątki:
 * - [Wykorzystanie viewModelScope, brak blokowania Main]
 *
 * 🧪 Testowalność:
 * - [Informacja o DI, brak singletonów, determinizm]
 *
 * 🧼 Lifecycle:
 * - [Zdarzenia startu/stopu, obserwacja sesji, czyszczenie zasobów]
 */
```

## Zasady pisania
1. **Zwięzłość**: Skracaj opisy o 10-20%, kładąc nacisk na fakty techniczne.
2. **Emoji**: Używaj standardowych emoji dla każdej sekcji, aby umożliwić szybkie skanowanie wzrokiem.
3. **Kontekst**: Jasno określaj granice między ViewModel a Repository (odpowiedzialność za dane).
4. **Senior/Staff level**: Dokumentacja powinna tłumaczyć "dlaczego" i "jakie są gwarancje", a nie tylko "co robi kod".
