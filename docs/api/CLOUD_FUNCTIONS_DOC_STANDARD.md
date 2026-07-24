# Standard Dokumentacji dla Cloud Functions

Cloud Functions stanowią logiczny backend aplikacji. Dokumentacja musi precyzyjnie opisywać wyzwalacze oraz wpływ na bazę danych.

## Szablon

```typescript
/**
 * 🎯 Cel: [Opis funkcji, np. przeliczanie statystyk miejsca]
 *
 * ⚡ Wyzwalacz (Trigger): [Np. onDocumentCreated, onSchedule, onRequest]
 *
 * 📥 Wejście (Payload): [Struktura danych wejściowych]
 *
 * ✅ Efekty uboczne: [Zmiany w innych kolekcjach, wysyłka e-mail/push]
 *
 * 🛡️ Bezpieczeństwo: [Weryfikacja ról, sprawdzanie UID, Custom Claims]
 *
 * ⚙️ Techniczne: [Timeouty, limity pamięci, obsługa transakcji]
 */
```
