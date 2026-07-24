# Standard Dokumentacji KDoc dla Komponentów Infrastrukturalnych

Komponenty infrastrukturalne (Sync, Analytics, Widgets, Utils) stanowią "krwiobieg" aplikacji. Ich dokumentacja musi skupiać się na wpływie na system, zasobach oraz bezpieczeństwie danych.

## Szablon (Template)

```kotlin
/**
 * 🎯 Odpowiedzialności:
 * - [Opis roli w systemie, np. zarządzanie analityką, synchronizacja w tle]
 *
 * 🛡️ Bezpieczeństwo i Prywatność:
 * - [Informacje o danych PII, logowaniu wrażliwych danych, anonimizacji]
 *
 * ⚡ Wydajność i Zasoby:
 * - [Wpływ na baterię, zużycie danych, threading (WorkManager/IO)]
 *
 * ✅ Gwarancje:
 * - [Idempotentność, trwałość danych (persistence), obsługa błędów]
 */
```

## Zasady pisania
1. **Anonimizacja**: Zawsze dokumentuj, czy dany komponent przesyła dane identyfikujące użytkownika (PII).
2. **Persistence**: Jeśli komponent zapisuje dane na dysku, określ mechanizm (Room, SharedPreferences, DataStore).
3. **Idempotentność**: Kluczowe dla mechanizmów Sync - czy operację można bezpiecznie powtórzyć?
