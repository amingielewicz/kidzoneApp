# ADR-002: Use Navigation Compose

## Status

Accepted

## Context

KidZone is built with Jetpack Compose. Navigation should be consistent with the UI stack and should support deep links, predictable back stack behavior and typed screen arguments.

## Decision

KidZone uses Navigation Compose as the main navigation mechanism.

## Consequences

Positive:

- navigation is aligned with Compose,
- screen routes are explicit,
- deep links can be handled in one navigation graph,
- UI state stays in ViewModels.

Trade-offs:

- route arguments must be kept small,
- complex flows require discipline around back stack handling,
- ViewModels must not receive NavController.

## Rules

- ViewModel does not know NavController.
- Large objects are not passed through routes.
- Screen arguments are validated.
- Deep links handle missing or invalid resources.
