# ADR-004: Use StateFlow for Screen State

## Status

Accepted

## Context

KidZone screens need predictable state handling for loading, success, empty, error and refresh states. Compose works best when UI is rendered from immutable state.

## Decision

KidZone uses `StateFlow` as the primary mechanism for exposing screen state from ViewModels.

One-time events should use a separate event stream, such as `SharedFlow`, instead of being stored as persistent UI state.

## Consequences

Positive:

- predictable UI rendering,
- single source of truth per screen,
- easy unit testing of ViewModels,
- clear separation between persistent state and one-time events.

Trade-offs:

- requires explicit UiState models,
- careless state updates may cause unnecessary recompositions,
- one-time events need a separate pattern.

## Rules

- Major screens expose `StateFlow<UiState>`.
- UiState should be immutable.
- Compose observes state and sends actions to ViewModel.
- Snackbar, toast and navigation events are not stored as persistent state.
