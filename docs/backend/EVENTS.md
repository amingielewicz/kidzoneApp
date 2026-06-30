# Backend Events

## Cel

Dokument opisuje zdarzenia backendowe KidZone oraz przepływy pomiędzy Firestore, Auth, Storage, Cloud Functions i FCM.

## Typy eventów

```text
Auth events
Firestore events
Storage-related events
Scheduled events
Manual admin actions
```

## Auth events

### User created

Źródło:

```text
Firebase Authentication
```

Skutek:

- utworzenie profilu użytkownika,
- ustawienie wartości domyślnych,
- opcjonalne wysłanie wiadomości powitalnej,
- opcjonalne powiadomienie administratora.

### User deleted

Źródło:

```text
Firebase Authentication
```

Skutek:

- oznaczenie profilu jako usunięty albo usunięcie danych,
- czyszczenie zależnych danych zgodnie z polityką produktu,
- opcjonalne powiadomienie administratora.

## Firestore events

### Review created

Źródło:

```text
reviews/{reviewId}
```

Skutek:

- aktualizacja `ratingAverage`,
- aktualizacja `reviewsCount`,
- sprawdzenie odznak,
- wysłanie powiadomienia do właściciela miejsca.

### Place report created

Źródło:

```text
reports/{reportId}
```

Gdy `targetType = place`.

Skutek:

- zapis zgłoszenia do kolejki moderacji,
- powiadomienie administratora,
- blokada duplikatu zgłoszenia, jeśli wymagana.

### Review report created

Źródło:

```text
reports/{reportId}
```

Gdy `targetType = review`.

Skutek:

- zapis zgłoszenia do kolejki moderacji,
- powiadomienie administratora,
- powiązanie zgłoszenia z opinią i miejscem.

### Photo report created

Źródło:

```text
reports/{reportId}
```

Gdy `targetType = photo`.

Skutek:

- zapis zgłoszenia do kolejki moderacji,
- powiadomienie administratora,
- przygotowanie akcji usunięcia zdjęcia.

### Change request created

Źródło:

```text
changeRequests/{changeRequestId}
```

Skutek:

- zapis propozycji zmiany,
- powiadomienie administratora,
- oczekiwanie na akceptację lub odrzucenie.

## Storage-related events

### Photo uploaded

Źródło:

```text
Firebase Storage
```

Skutek:

- aktualizacja listy zdjęć miejsca,
- zwiększenie `photosCount`,
- opcjonalne powiadomienie obserwujących albo właściciela miejsca.

## Scheduled events

### Daily ranking check

Źródło:

```text
Cloud Scheduler
```

Skutek:

- przeliczenie albo sprawdzenie zmian rankingowych,
- wysłanie powiadomień o awansie,
- zapis metadanych ostatniego sprawdzenia.

## Manual admin actions

### Delete place

Źródło:

```text
Admin panel HTTP call
```

Skutek:

- usunięcie lub oznaczenie miejsca jako usunięte,
- zapis powodu,
- opcjonalne powiadomienie autora.

### Delete review

Źródło:

```text
Admin panel HTTP call
```

Skutek:

- usunięcie opinii,
- aktualizacja agregatów miejsca,
- opcjonalne powiadomienie autora.

### Delete photo

Źródło:

```text
Admin panel HTTP call
```

Skutek:

- usunięcie pliku ze Storage,
- aktualizacja dokumentu miejsca,
- zamknięcie zgłoszenia, jeśli dotyczy.

## Zasady projektowe

- Eventy powinny być idempotentne, jeśli mogą zostać wykonane ponownie.
- Eventy nie powinny zakładać idealnej kolejności wykonania.
- Event powinien mieć jasno opisany skutek uboczny.
- Eventy krytyczne powinny mieć logi diagnostyczne bez danych wrażliwych.

## Checklist

- [ ] Event ma określone źródło.
- [ ] Event ma określony skutek.
- [ ] Event nie loguje danych wrażliwych.
- [ ] Event jest odporny na ponowne wykonanie albo ma zabezpieczenie.
- [ ] Event ma scenariusz testowy.
