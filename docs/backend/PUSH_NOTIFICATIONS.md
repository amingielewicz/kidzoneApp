# Push Notifications

## Cel

Dokument opisuje architekturę powiadomień push w KidZone oraz zasady ich projektowania.

## Technologia

- Firebase Cloud Messaging (FCM)
- Cloud Functions jako warstwa wysyłająca
- Firebase Authentication do identyfikacji użytkownika

## Typy powiadomień

- Nowa opinia o miejscu
- Nowe zdjęcie miejsca
- Zdobycie odznaki
- Awans w rankingu
- Informacje administracyjne
- Komunikaty maintenance (opcjonalnie)

## Zasady

- Wysyłamy tylko istotne powiadomienia.
- Nie wysyłamy wielu identycznych powiadomień.
- Szanujemy ustawienia użytkownika.
- Powiadomienia powinny prowadzić do właściwego ekranu (deep link).

## Payload

Powinien zawierać:

```text
notification
  title
  body

data
  type
  targetId
  targetType
```

## Bezpieczeństwo

- Tokeny FCM nie są publiczne.
- Tokeny usuwamy po usunięciu konta.
- Nie logujemy pełnych tokenów.

## Monitoring

- liczba wysłanych push,
- błędy dostarczenia,
- nieważne tokeny,
- czas realizacji.

## Checklist

- [ ] Powiadomienie ma właściciela biznesowego.
- [ ] Deep link działa.
- [ ] Szanowane są preferencje użytkownika.
- [ ] Brak danych wrażliwych w payload.
- [ ] Obsłużono nieważne tokeny.
