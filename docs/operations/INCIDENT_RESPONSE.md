# Incident Response Handbook

## Cel

Dokument opisuje reakcję na incydenty produkcyjne KidZone.

## Co jest incydentem

Incydentem jest sytuacja, która wpływa na użytkowników, bezpieczeństwo, dane albo koszty.

Przykłady:

- aplikacja crashuje po starcie,
- logowanie nie działa,
- mapa nie działa,
- dodawanie miejsca nie działa,
- Firestore Rules blokują poprawnych użytkowników,
- Firestore Rules pozwalają na nieuprawniony dostęp,
- Storage upload nie działa,
- koszty Firebase albo Maps rosną nietypowo,
- release loguje dane wrażliwe,
- pojawia się luka privacy/security.

## Priorytety

### P0

Krytyczny problem produkcyjny:

- aplikacja nie uruchamia się,
- login nie działa globalnie,
- utrata danych,
- wyciek danych,
- krytyczny problem security.

### P1

Poważny problem:

- główne flow działa częściowo,
- mapa lub lista działa niestabilnie,
- znaczny wzrost crash rate,
- problem dotyczy dużej grupy użytkowników.

### P2

Istotny problem bez natychmiastowej blokady:

- pojedynczy ekran ma regresję,
- problem dotyczy małej grupy użytkowników,
- istnieje obejście.

## Pierwsze 15 minut

- [ ] Potwierdzić problem.
- [ ] Ustalić zakres: wersja, urządzenia, użytkownicy.
- [ ] Sprawdzić Crashlytics.
- [ ] Sprawdzić Google Play Console.
- [ ] Sprawdzić Firebase Status.
- [ ] Sprawdzić ostatnie PR-y i release.
- [ ] Ustalić priorytet P0/P1/P2.
- [ ] Zdecydować: rollback, hotfix, config change albo monitorowanie.

## Rollback

Rollback rozważyć, gdy:

- crash rate gwałtownie rośnie,
- podstawowe flow nie działa,
- App Check lub Rules blokują produkcję,
- występuje ryzyko privacy/security,
- hotfix nie jest szybki.

## Hotfix

Hotfix wymaga:

- [ ] minimalnego code review,
- [ ] testu naprawianego flow,
- [ ] signed release build,
- [ ] aktualizacji release notes,
- [ ] obserwacji Crashlytics po wdrożeniu.

## Komunikacja

Dla każdego większego incydentu zapisać:

- co się stało,
- od kiedy trwało,
- ilu użytkowników dotyczyło,
- jak naprawiono,
- co zapobiega powtórce.

## Postmortem

Po P0/P1 przygotować krótkie postmortem:

```text
Data:
Priorytet:
Opis:
Wpływ:
Przyczyna:
Naprawa:
Działania zapobiegawcze:
Powiązane issue / PR:
```

## Checklist zamknięcia incydentu

- [ ] Problem rozwiązany.
- [ ] Crashlytics / monitoring potwierdza poprawę.
- [ ] Użytkownicy nie zgłaszają nowych przypadków.
- [ ] Utworzono follow-up issue.
- [ ] Postmortem zapisane dla P0/P1.
