# ADR-001: Use Clean Architecture

## Status

Accepted

## Context

KidZone contains UI, business rules, Firebase integrations, local cache and platform-specific Android code. Without clear boundaries, the project becomes harder to test and maintain.

## Decision

KidZone uses Clean Architecture principles with the following logical layers:

```text
Presentation
Domain
Data
Framework
```

Domain remains the most stable layer and must not depend on Android, Firebase, Compose or Room.

## Consequences

Positive:

- better testability,
- clearer ownership of logic,
- easier replacement of data sources,
- lower risk of framework leakage into business rules.

Trade-offs:

- more mapper code,
- more interfaces,
- more discipline during review.

## Rules

- UI does not call Firebase directly.
- ViewModel uses use cases or repository interfaces.
- Data implements repository contracts.
- DTO and entities do not leak into UI.
