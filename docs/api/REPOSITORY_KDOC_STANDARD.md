# Standard Dokumentacji KDoc dla Repozytoriów

Repozytoria w projekcie kidZone stanowią fundament warstwy danych. Ich dokumentacja musi precyzyjnie określać zasady synchronizacji, zachowanie w trybie offline oraz gwarancje spójności.

## Szablon (Template)

```kotlin
/**
 * 🎯 Odpowiedzialności:
 * - [Główny cel repozytorium, np. zarządzanie danymi o miejscach]
 * - [Koordynacja między Firestore a lokalnym cache Room]
 *
 * 🔌 Strategia Cache:
 * - [Np. Single Source of Truth (Room), Read-through, Write-through]
 *
 * 🛡️ Autoryzacja i Bezpieczeństwo:
 * - [Kto może wywoływać metody, jakie uprawnienia są wymagane]
 *
 * ✅ Gwarancje spójności:
 * - [Np. atomowość operacji, zachowanie liczników, obsługa konfliktów]
 *
 * 📤 Mapowanie błędów:
 * - [Jak techniczne błędy (np. FirebaseException) są mapowane na domeny]
 *
 * 🧵 Threading:
 * - [Np. bezpieczne wywołania z dowolnego wątku (Dispatcher.IO)]
 */
```

## Zasady pisania
1. **Bezpieczeństwo**: Jasno określaj, czy dana operacja wymaga zalogowanego użytkownika.
2. **Offline-first**: Dokumentuj, co dzieje się, gdy urządzenie nie ma sieci (np. "zapis do lokalnej kolejki operacji oczekujących").
3. **Techniczne detale**: Wspominaj o ważnych mechanizmach (np. transakcje, batch updates).
