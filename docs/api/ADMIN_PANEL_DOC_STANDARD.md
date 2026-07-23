# Standard Dokumentacji dla Panelu Administracyjnego (React)

Dokumentacja komponentów i usług w Panelu Admina musi zapewniać przejrzystość w zakresie przepływu danych oraz rygorystycznych zasad bezpieczeństwa (tylko dla adminów).

## Szablon dla Komponentów/Stron

```typescript
/**
 * 🎯 Odpowiedzialności:
 * - [Główny cel komponentu, np. wyświetlanie statystyk dashboardu]
 *
 * 📥 Wejście (Props/Context):
 * - [Dane wejściowe, parametry URL, dane z AuthContext]
 *
 * 📤 Wyjście (Events/Navigation):
 * - [Akcje użytkownika, nawigacja do innych stron]
 *
 * ⚡ Zarządzanie stanem:
 * - [Użyte hooki (useState, useEffect), zapytania do Firestore]
 *
 * 🛡️ Bezpieczeństwo:
 * - [Wymagana rola admina, ochrona tras (routes)]
 */
```

## Szablon dla Usług (Services/API)

```typescript
/**
 * 🎯 Cel: [Np. komunikacja z Cloud Functions]
 * 📥 Parametry: [Opis argumentów]
 * 📤 Zwraca: [Typ zwracany, Promise]
 * 🛡️ Autoryzacja: [Np. automatyczne dołączanie tokena Bearer]
 */
```
