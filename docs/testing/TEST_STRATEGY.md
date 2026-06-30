# Test Strategy

## Cel

Dokument definiuje strategię testowania projektu KidZone oraz odpowiedzialności poszczególnych poziomów testów.

## Piramida testów

- Unit Tests
- Integration Tests
- UI / Instrumentation Tests
- Manual Exploratory Tests
- Release Smoke Tests

## Zakres

### Unit
- ViewModel
- Use Cases
- Repository logic
- Mappers
- Validators

### Integration
- Firebase Emulator
- Room
- Repository
- Cloud Functions

### UI
- Kluczowe ścieżki użytkownika
- Nawigacja
- Formularze
- Uprawnienia

### Manual
- UX
- Accessibility
- Offline
- Powiadomienia
- Deep Links

## Definition of Done

- Nowe funkcje mają odpowiedni poziom testów.
- Błędy regresyjne otrzymują test odtwarzający problem.
- Krytyczne ścieżki są objęte smoke testami.

## Checklist

- [ ] Unit tests
- [ ] Integration tests
- [ ] UI tests (jeśli wymagane)
- [ ] Manual QA
- [ ] Smoke test po release