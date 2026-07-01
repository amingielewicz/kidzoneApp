# ADR-005: Use Offline-First Strategy for Read-Heavy Screens

## Status

Accepted

## Context

KidZone depends on maps, lists, place details and user-generated content. Mobile users can have unstable network connection, so key screens should remain usable when possible.

## Decision

KidZone adopts an offline-first strategy for read-heavy screens where local cache improves reliability and user experience.

Preferred flow:

```text
Room cache -> UI
Firebase snapshot -> Room update -> UI refresh
```

Writes can be online-only or queued depending on feature risk and product requirements.

## Consequences

Positive:

- better perceived performance,
- better user experience with weak network,
- fewer empty screens during temporary outages,
- clearer separation between local and remote data.

Trade-offs:

- cache invalidation is harder,
- conflict resolution must be designed per feature,
- queued writes require idempotency,
- tests must cover online and offline states.

## Rules

- Lists should prefer cached data when available.
- UI must clearly indicate offline state.
- Writes must avoid duplicate data.
- Conflicts are resolved according to product rules.
- Offline behaviour must be covered by QA scenarios.
