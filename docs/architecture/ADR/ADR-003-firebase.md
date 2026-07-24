# ADR-003: Use Firebase as Backend Platform

Ostatnia aktualizacja: 2026-07-14

## Status

Accepted

## Context

kidZone potrzebuje uwierzytelniania, bazy danych, przechowywania plików, konfiguracji zdalnej, powiadomień, diagnostyki i analityki. Utrzymanie własnego backendu dla wszystkich tych funkcji zwiększałoby koszt i złożoność projektu.

## Decision

Firebase pozostaje główną platformą backendową.

Używane lub planowane usługi:

- Firebase Authentication,
- Cloud Firestore,
- Firebase Storage,
- Cloud Functions,
- Remote Config,
- Cloud Messaging,
- Crashlytics,
- Analytics,
- Performance Monitoring,
- App Check,
- Hosting.

## Rules

- Firebase SDK nie jest wywoływane bezpośrednio z UI,
- publiczne i prywatne dane są rozdzielone,
- Firestore i Storage Rules są częścią release gate,
- zapytania mają limity, paginację i indeksy,
- eventy i retry są idempotentne,
- App Check nie zastępuje auth, Rules i rate limitingu,
- błędy SDK są mapowane przed UI,
- logi i telemetryka nie zawierają PII,
- account deletion obejmuje wszystkie aktywne usługi,
- koszty, quota i billing są monitorowane.

## Consequences

### Positive

- szybsze dostarczanie funkcji,
- zarządzana infrastruktura,
- dobre wsparcie Androida,
- gotowe auth, push i monitoring,
- mniejszy koszt operacyjny na wczesnym etapie.

### Trade-offs

- vendor lock-in,
- ograniczenia modelu zapytań Firestore,
- ryzyko kosztów przy wzroście ruchu,
- konieczność rygorystycznych Rules i testów,
- zależność release od konfiguracji konsoli,
- migracje wymagają zgodności z aktywnymi buildami.

## Rejected alternatives

### Własny backend od początku

Odrzucony z powodu większego kosztu utrzymania, dłuższego time-to-market i dodatkowego zakresu bezpieczeństwa.

### Bezpośredni dostęp klientów bez Rules i funkcji zaufanych

Odrzucony z powodu ryzyka eskalacji uprawnień, manipulacji agregatami i niekontrolowanych kosztów.

## Review trigger

Decyzję należy ponownie ocenić, gdy:

- koszt Firebase staje się dominujący,
- model danych wymaga zapytań trudnych do realizacji,
- wymagania prawne lub hostingowe zmieniają się,
- pojawia się potrzeba niezależnego backendu dla krytycznych operacji.
