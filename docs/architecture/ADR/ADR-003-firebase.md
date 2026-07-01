# ADR-003: Use Firebase as Backend Platform

## Status

Accepted

## Context

KidZone needs authentication, database, file storage, remote configuration, crash reporting, analytics and push notifications. Building and maintaining a custom backend for all these areas would slow down development.

## Decision

KidZone uses Firebase as the primary backend platform.

Main services:

- Firebase Authentication,
- Cloud Firestore,
- Firebase Storage,
- Remote Config,
- Cloud Messaging,
- Crashlytics,
- Analytics,
- Performance Monitoring.

## Consequences

Positive:

- faster delivery,
- managed infrastructure,
- Android-friendly SDKs,
- built-in auth and push notifications,
- good fit for MVP and early production.

Trade-offs:

- vendor lock-in,
- Security Rules require strong discipline,
- Firestore query model affects data modeling,
- costs must be monitored as usage grows.

## Rules

- Firestore queries must use limits.
- Public and private data must be separated.
- Security Rules are part of release readiness.
- Firebase errors are mapped before reaching UI.
- Indexes are versioned and deployed before release.
