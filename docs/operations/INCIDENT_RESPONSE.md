# Incident Response Handbook

Ostatnia aktualizacja: 2026-07-13

## Cel

Procedura reagowania na incydenty produkcyjne kidZone wpływające na użytkowników, dane, bezpieczeństwo, dostępność albo koszty.

## Przykłady incydentów

- crash przy starcie,
- globalny problem logowania,
- niedostępna mapa lub lista,
- uszkodzone dodawanie miejsca albo zdjęć,
- account deletion nie działa lub usuwa dane częściowo,
- Rules blokują poprawnych użytkowników,
- Rules albo Storage umożliwiają nieuprawniony dostęp,
- App Check blokuje prawidłowe buildy,
- rollout powoduje wzrost crash/ANR,
- release loguje dane wrażliwe,
- nietypowy wzrost kosztów Firebase lub Maps.

## Priorytety

### P0

- aplikacja nie uruchamia się,
- logowanie nie działa globalnie,
- utrata lub wyciek danych,
- aktywna krytyczna luka bezpieczeństwa,
- niekontrolowany wzrost kosztów z realnym ryzykiem finansowym.

### P1

- główne flow działa częściowo,
- duża grupa użytkowników ma problem,
- mapa, lista, zdjęcia lub account deletion działają niestabilnie,
- znaczący wzrost crash rate lub ANR.

### P2

- problem ograniczony do jednego ekranu lub małej grupy,
- istnieje bezpieczne obejście,
- brak ryzyka utraty danych i bezpieczeństwa.

## Pierwsza reakcja

- [ ] potwierdź problem,
- [ ] ustal wersję, build i track,
- [ ] określ zakres urządzeń i użytkowników,
- [ ] sprawdź Crashlytics i Android vitals,
- [ ] sprawdź Firebase/GCP status i usage,
- [ ] przejrzyj ostatnie PR-y, rollout i Remote Config,
- [ ] oceń wpływ privacy/security,
- [ ] nadaj priorytet,
- [ ] wybierz: monitorowanie, config change, halt rollout, rollback lub hotfix.

## Ograniczenie skutków

Możliwe działania:

- zatrzymanie staged rollout,
- wyłączenie funkcji przez Remote Config,
- ograniczenie ruchu lub kosztownej funkcji,
- wycofanie reguł lub konfiguracji,
- rotacja sekretu,
- zablokowanie podatnego endpointu,
- komunikat maintenance.

Każde działanie musi mieć właściciela i zapisany moment wykonania.

## Hotfix

Hotfix wymaga:

- minimalnego review,
- testu poprawianego flow,
- regresji obszaru ryzyka,
- zwiększenia `VERSION_CODE`,
- signed AAB,
- aktualizacji release notes,
- monitoringu po wdrożeniu.

## Incydent privacy/security

Dodatkowo:

- zachowaj dowody i logi bez kopiowania danych osobowych do issue,
- ogranicz dostęp do szczegółów,
- określ zakres danych i użytkowników,
- obróć ujawnione sekrety,
- oceń obowiązek powiadomienia użytkowników lub organu,
- nie zamykaj incydentu bez działań zapobiegawczych.

## Komunikacja

Zapisz:

- co się stało,
- kiedy problem się rozpoczął i zakończył,
- jaki był wpływ,
- jakie działania wykonano,
- czy istnieje obejście,
- kiedy nastąpi kolejna aktualizacja,
- co zapobiegnie powtórce.

## Postmortem P0/P1

```text
Data:
Priorytet:
Wersja / build:
Opis:
Wpływ:
Oś czasu:
Przyczyna źródłowa:
Działania ograniczające:
Naprawa:
Działania zapobiegawcze:
Powiązane issue / PR:
```

Postmortem powinno być bez winy personalnej i skupione na systemie, kontroli oraz testach.

## Zamknięcie

- [ ] problem rozwiązany,
- [ ] monitoring potwierdza poprawę,
- [ ] rollout ma świadomą decyzję,
- [ ] dane i koszty zostały zweryfikowane,
- [ ] follow-up issues utworzone,
- [ ] postmortem zapisane dla P0/P1,
- [ ] test lub kontrola zapobiegawcza dodana,
- [ ] dokumentacja została zaktualizowana.
