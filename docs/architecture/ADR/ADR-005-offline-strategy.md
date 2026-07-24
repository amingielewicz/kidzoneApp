# ADR-005: Offline strategy for read-heavy screens

Ostatnia aktualizacja: 2026-07-14

## Status

Accepted with gated offline writes

## Context

kidZone korzysta z mapy, list, szczegółów miejsc, opinii i widgetu. Użytkownicy mogą mieć niestabilne połączenie, ale błędna synchronizacja zapisów mogłaby tworzyć duplikaty, konflikty i utratę danych.

## Decision

Dla ekranów odczytowych stosujemy lokalny cache Room, a Firebase pozostaje źródłem prawdy.

```text
Room cache → UI
Firebase → Repository → Room → UI refresh
```

Zapisy pozostają online-first, dopóki replay przez `SyncWorker` nie spełni wymagań produkcyjnych.

## Current behavior

- miejsca i opinie mogą być odczytane z cache,
- Start, Lista, Mapa, szczegóły i widget korzystają z danych lokalnych,
- brak cache kończy się kontrolowanym empty/error state,
- zapis offline zwraca błąd i nie udaje sukcesu,
- `pending_operations` i worker istnieją, ale replay jest zablokowany,
- prywatny stan jest czyszczony po logout, ban sign-out i account deletion.

## Rules

- cache nie jest źródłem prawdy dla trwałych zapisów,
- UI jasno rozróżnia offline, stale data i błąd,
- retry nie tworzy duplikatu,
- zapis nie jest oznaczony jako sukces bez potwierdzenia backendu,
- pending operation nie wykonuje się po zakończeniu sesji użytkownika,
- dane prywatne nie pozostają w cache ani widgetach,
- konflikty są projektowane per typ operacji,
- cache i replay mają testy jednostkowe oraz integracyjne.

## Conditions for enabling queued writes

- kompletne processory add/update/delete,
- idempotency keys,
- status pending/failed widoczny w UI,
- backoff i dead-letter,
- obsługa wygasłej sesji,
- reguły konfliktów,
- cleanup po logout/delete,
- testy z Firebase Emulator,
- monitoring i możliwość wyłączenia funkcji.

## Consequences

### Positive

- lepsza użyteczność przy słabej sieci,
- szybsze renderowanie wcześniej pobranych danych,
- mniejsza liczba pustych ekranów,
- prostszy model integralności niż pełne offline-first writes.

### Trade-offs

- cache invalidation i stale data,
- większa złożoność repository,
- konieczność testowania wielu stanów sieci,
- brak pełnego zapisu offline,
- przyszły replay wymaga migracji i dodatkowego UX.

## Rejected alternatives

### Pełne offline-first writes od początku

Odrzucone z powodu braku kompletnego replay, konflikt resolution i obserwowalności.

### Wyłącznie Firestore offline persistence

Niewystarczające jako jedyna strategia, ponieważ widget, jawny cache, zapytania mapy i kontrola prywatnego stanu wymagają lokalnego modelu aplikacji.

## Related documentation

- `docs/android/OFFLINE_MODE.md`
- `docs/architecture/DATA_FLOW.md`
- `docs/architecture/STATE_MANAGEMENT.md`
