# Firestore Schema

## Cel

Dokument opisuje główne kolekcje Firestore używane przez KidZone, ich odpowiedzialności oraz zasady modelowania danych.

## Zasady ogólne

- Dokumenty publiczne nie powinny zawierać danych wrażliwych.
- Dane prywatne użytkownika powinny być oddzielone od publicznego profilu.
- Listy i rankingi powinny korzystać z pól denormalizowanych.
- Zapytania muszą mieć limity.
- Pola używane do sortowania i filtrowania muszą mieć indeksy.
- Struktura danych powinna wspierać offline cache po stronie aplikacji.

## Kolekcje główne

```text
places
users
reviews
reports
changeRequests
badges
notifications
appConfig
```

## places

Reprezentuje miejsce przyjazne dzieciom.

Przykładowe pola:

```text
id: string
name: string
normalizedName: string
category: string
amenities: string[]
description: string?
address: string
location: geopoint
geohash: string?
createdBy: string
createdAt: timestamp
updatedAt: timestamp
ratingAverage: number
reviewsCount: number
photosCount: number
status: string
isDeleted: boolean
```

### Zasady

- `ratingAverage` i `reviewsCount` są polami denormalizowanymi.
- Lista i ranking nie powinny pobierać wszystkich opinii dla każdego miejsca.
- `status` pozwala obsługiwać moderację.
- `isDeleted` może wspierać soft delete, jeśli wymagane przez proces.

## users

Reprezentuje publiczny profil użytkownika.

Przykładowe pola:

```text
id: string
displayName: string
avatarUrl: string?
createdAt: timestamp
updatedAt: timestamp
placesCount: number
reviewsCount: number
badgesCount: number
rankScore: number
isBanned: boolean
```

### Zasady

- Nie przechowujemy publicznie e-maila, tokenów ani danych prywatnych.
- Dane prywatne powinny być w oddzielnej strukturze z ostrzejszymi Rules.

## reviews

Reprezentuje opinię o miejscu.

Przykładowe pola:

```text
id: string
placeId: string
userId: string
rating: number
comment: string?
photoUrls: string[]
createdAt: timestamp
updatedAt: timestamp
status: string
isDeleted: boolean
```

### Zasady

- `rating` powinien mieć zakres 1-5.
- Jedna opinia użytkownika dla jednego miejsca powinna być kontrolowana regułą lub logiką aplikacji.
- Dodanie opinii powinno aktualizować pola denormalizowane miejsca.

## reports

Reprezentuje zgłoszenia naruszeń.

Przykładowe pola:

```text
id: string
targetType: string
targetId: string
reason: string
description: string?
createdBy: string
createdAt: timestamp
status: string
resolvedBy: string?
resolvedAt: timestamp?
```

### Zasady

- Zgłoszenia powinny być dostępne dla admina.
- Użytkownik nie powinien móc czytać cudzych zgłoszeń.
- Powinna istnieć ochrona przed duplikatem zgłoszenia tego samego targetu przez tego samego użytkownika.

## changeRequests

Reprezentuje propozycję zmiany danych miejsca.

Przykładowe pola:

```text
id: string
placeId: string
requestedBy: string
changes: map
status: string
createdAt: timestamp
reviewedBy: string?
reviewedAt: timestamp?
rejectReason: string?
```

## badges

Reprezentuje odznaki użytkowników.

Przykładowe pola:

```text
id: string
userId: string
badgeType: string
earnedAt: timestamp
sourceType: string?
sourceId: string?
```

## notifications

Reprezentuje powiadomienia aplikacyjne lub metadane push.

Przykładowe pola:

```text
id: string
userId: string
type: string
title: string
body: string
createdAt: timestamp
readAt: timestamp?
targetType: string?
targetId: string?
```

## appConfig

Konfiguracja aplikacji, jeśli część ustawień nie jest trzymana w Remote Config.

Przykładowe pola:

```text
maintenanceMode: boolean
minimumSupportedVersion: string
latestVersion: string
updatedAt: timestamp
```

## Checklist schema review

- [ ] Każda kolekcja ma właściciela odpowiedzialności.
- [ ] Pola publiczne i prywatne są rozdzielone.
- [ ] Pola rankingowe są denormalizowane.
- [ ] Zapytania list mają limity.
- [ ] Zapytania sortowane mają indeksy.
- [ ] Rules walidują typy i ownership.
- [ ] Dane wrażliwe nie trafiają do publicznych dokumentów.
