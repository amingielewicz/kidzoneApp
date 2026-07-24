# Firestore Schema

Ostatnia aktualizacja: 2026-07-13

## Cel

Dokument opisuje główne kolekcje Firestore używane przez kidZone, ich odpowiedzialności, relacje, ownership, pola systemowe i wpływ na prywatność oraz account deletion.

## Zasady ogólne

- dane publiczne i prywatne są rozdzielone,
- publiczne dokumenty nie zawierają PII,
- zapytania listowe mają limity i paginację,
- pola używane do sortowania i filtrowania mają indeksy,
- pola agregowane są aktualizowane po stronie zaufanej,
- klient nie może zmieniać ról, statusów moderacji ani liczników,
- retry zapisów jest idempotentne,
- schemat wspiera cache Room i kontrolowaną synchronizację.

## Główne kolekcje

```text
users
places
reviews
reports
changeRequests
badges
notifications
appConfig
```

Dane prywatne użytkownika:

```text
users/{uid}/private/profile
users/{uid}/private/messaging
users/{uid}/private/preferences
```

## users

Publiczny profil użytkownika.

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
status: string
```

Zasady:

- brak e-maila i tokenów FCM,
- `isBanned`, `rankScore` i liczniki są chronione,
- właściciel edytuje tylko dozwolone pola,
- po account deletion profil jest usuwany lub anonimizowany zgodnie z polityką.

## users/{uid}/private/profile

Przykładowe pola:

```text
email: string?
firstName: string?
lastName: string?
createdAt: timestamp
updatedAt: timestamp
```

Dostęp ma właściciel albo upoważniony administrator. Dokument podlega pełnemu cleanup przy usunięciu konta.

## users/{uid}/private/messaging

Przykładowe pola:

```text
fcmTokens: map | array
notificationsEnabled: boolean
notificationPreferences: map
updatedAt: timestamp
```

Tokeny FCM są prywatne, nie są logowane i są usuwane lub unieważniane po logout oraz delete account.

## places

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

Zasady:

- `createdBy` odpowiada właścicielowi,
- agregaty i status moderacji są chronione,
- lista, ranking i mapa używają limitów,
- usunięcie miejsca uwzględnia opinie, zdjęcia, zgłoszenia i cache,
- soft delete wymaga jawnego filtra we wszystkich publicznych zapytaniach.

## reviews

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

Zasady:

- `rating` ma zakres 1–5,
- `userId` odpowiada zalogowanemu użytkownikowi,
- pola moderacyjne są chronione,
- zapis aktualizuje agregaty miejsca po stronie zaufanej,
- duplikaty opinii dla tego samego użytkownika i miejsca są kontrolowane,
- po account deletion opinia jest usuwana albo anonimizowana zgodnie z polityką.

## reports

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

Zasady:

- autor tworzy zgłoszenie,
- klient nie ustawia pól rozpatrzenia,
- cudze zgłoszenia nie są publicznie czytelne,
- rekord nie powinien kopiować zbędnych danych celu,
- duplikaty są ograniczane.

## changeRequests

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

`changes` musi mieć walidowany zakres dozwolonych pól. Status i pola administracyjne są ustawiane po stronie zaufanej.

## badges

Przykładowe pola:

```text
id: string
userId: string
badgeType: string
earnedAt: timestamp
sourceType: string?
sourceId: string?
```

Odznaki są tworzone przez backend lub kontrolowaną logikę. Klient nie może samodzielnie przyznać odznaki.

## notifications

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
notificationId: string?
```

Zasady:

- dokument jest prywatny dla odbiorcy,
- payload jest minimalny,
- pełne dane celu są pobierane po `targetId`,
- zapis systemowy odbywa się po stronie zaufanej,
- retencja i cleanup są jawnie określone.

## appConfig

Przykładowe pola:

```text
maintenanceMode: boolean
minimumSupportedVersion: string
latestVersion: string
updatedAt: timestamp
```

Nie przechowujemy tu sekretów, ról ani danych administracyjnych przeznaczonych wyłącznie dla backendu.

## Pola systemowe i agregaty

Pola takie jak:

- `ratingAverage`,
- `reviewsCount`,
- `photosCount`,
- `rankScore`,
- statusy moderacji,
- liczniki aktywności,

mają właściciela aktualizacji po stronie backendu lub zaufanej transakcji. Każdy agregat wymaga testów i strategii naprawy niespójności.

## Migracje

Zmiana schematu powinna określać:

- kompatybilność ze starszym buildem,
- wartości domyślne,
- kolejność deploy backendu, Rules, indeksów i aplikacji,
- migrację istniejących dokumentów,
- wpływ na cache Room,
- możliwość rollbacku.

## Account deletion

Dla każdej kolekcji należy określić:

- usunięcie,
- anonimizację,
- zachowanie z uzasadnionego powodu,
- cleanup Storage i FCM,
- aktualizację agregatów,
- zachowanie po częściowym błędzie,
- wpływ backupów i retencji.

## Checklista schema review

- [ ] publiczne i prywatne dane są rozdzielone,
- [ ] pola systemowe są chronione,
- [ ] ownership odpowiada Rules,
- [ ] zapytania mają limity i indeksy,
- [ ] agregaty mają właściciela aktualizacji,
- [ ] retry nie tworzy duplikatów,
- [ ] migracja uwzględnia starsze buildy,
- [ ] account deletion jest opisane,
- [ ] dane wrażliwe nie trafiają do publicznych dokumentów.
