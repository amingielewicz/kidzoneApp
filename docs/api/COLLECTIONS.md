# Firestore Collections

## Przegląd

Dokument opisuje odpowiedzialność każdej kolekcji oraz relacje pomiędzy nimi.

| Kolekcja | Odpowiedzialność | Powiązania |
|----------|------------------|------------|
| users | Profil użytkownika | reviews, places, badges, notifications |
| places | Miejsca | reviews, reports, changeRequests |
| reviews | Opinie | users, places |
| reports | Zgłoszenia | places, reviews, photos |
| changeRequests | Propozycje zmian | places |
| badges | Odznaki | users |
| notifications | Powiadomienia | users |
| appConfig | Konfiguracja aplikacji | global |

## Relacje

```text
User
 ├── Places
 ├── Reviews
 ├── Badges
 └── Notifications

Place
 ├── Reviews
 ├── Reports
 └── Change Requests
```

## Zasady projektowe

- Brak joinów – dane pobierane przez referencje i pola denormalizowane.
- Każda kolekcja ma jednego właściciela biznesowego.
- Nie przechowujemy danych, które można bezpiecznie wyliczyć, chyba że służą wydajności.
- Zapytania listowe zawsze mają limit i paginację.

## Ownership

### users
- właściciel może edytować własny profil,
- administrator może wykonywać operacje moderacyjne.

### places
- autor może edytować zgodnie z polityką produktu,
- administrator ma pełne uprawnienia.

### reviews
- autor zarządza własną opinią,
- administrator może moderować lub usuwać.

### reports
- autor tworzy zgłoszenie,
- administrator rozpatruje zgłoszenie.

## Checklist

- [ ] Każda kolekcja ma jasno określoną odpowiedzialność.
- [ ] Relacje są udokumentowane.
- [ ] Ownership jest zgodny z Firestore Rules.
- [ ] Nie występują cykliczne zależności wymagające joinów.
