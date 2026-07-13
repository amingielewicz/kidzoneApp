# Monitoring Handbook

Ostatnia aktualizacja: 2026-07-13

## Cel

Dokument opisuje monitoring kidZone po stronie Androida, Firebase, Google Play i procesu release.

## Narzędzia

- Firebase Crashlytics,
- Firebase Performance Monitoring,
- Firebase Analytics,
- Firebase Remote Config,
- Google Play Console i Android vitals,
- Firebase/GCP usage i billing,
- GitHub Actions.

## Crashlytics

Crashlytics powinien umożliwiać ustalenie:

- wersji i builda,
- dotkniętej funkcji,
- skali problemu,
- momentu rozpoczęcia regresji,
- skuteczności hotfixa.

Raporty nie mogą zawierać danych osobowych, dokładnej lokalizacji, tokenów, treści formularzy ani URI zdjęć.

## Performance Monitoring

Monitorowane operacje:

- cold start,
- ładowanie Startu,
- lista miejsc,
- miejsca w pobliżu,
- mapa i markery,
- wyszukiwanie,
- ranking,
- zapis miejsca,
- upload zdjęcia,
- synchronizacja offline.

Trace powinny mieć stabilne nazwy i bezpieczne atrybuty.

## Analytics

Analytics mierzy zachowanie produktu, nie dane użytkownika.

Przykładowe eventy:

- onboarding ukończony,
- miejsce otwarte,
- filtr zastosowany,
- mapa otwarta,
- miejsce dodane,
- opinia dodana,
- ranking otwarty.

Nie wysyłamy tekstu opinii, wyszukiwanych fraz mogących identyfikować użytkownika, e-maili, tokenów ani dokładnej lokalizacji.

## Remote Config

Parametry wymagające obserwacji:

- limity markerów,
- rozmiar strony listy,
- limit rankingu,
- debounce wyszukiwania i mapy,
- maintenance mode,
- feature flags,
- limity uploadu i zdjęć.

Zmiana parametru krytycznego powinna mieć właściciela, opis ryzyka i możliwość rollbacku.

## Alerty

Alert lub regularna kontrola jest wymagana dla:

- wzrostu crash rate,
- wzrostu ANR,
- błędów logowania,
- błędów mapy i listy,
- nieudanych uploadów,
- błędów account deletion,
- problemów z uprawnieniami,
- błędów Cloud Functions,
- wzrostu kosztów Firebase i Maps.

## Monitoring rollout

Przy staged rollout sprawdź po każdym etapie:

- Android vitals,
- Crashlytics,
- błędy logowania,
- błędy mapy, listy i zdjęć,
- błędy usuwania konta,
- koszty i limity,
- zgłoszenia użytkowników.

Nie zwiększaj rollout, gdy pojawił się nowy P0/P1 albo wyraźny wzrost crash/ANR.

## Po release

- [ ] pierwsza kontrola po uruchomieniu rollout,
- [ ] kontrola po 24 godzinach,
- [ ] Performance i Analytics przejrzane,
- [ ] Firebase i Maps usage sprawdzone,
- [ ] opinie i zgłoszenia przejrzane,
- [ ] follow-up issues utworzone,
- [ ] decyzja o zwiększeniu, zatrzymaniu lub wycofaniu rollout zapisana.

## Incident checklist

- [ ] ustal wersję, build i track,
- [ ] określ zakres użytkowników,
- [ ] sprawdź Crashlytics i vitals,
- [ ] sprawdź Firebase/GCP usage,
- [ ] przejrzyj ostatnie wdrożenia i Remote Config,
- [ ] oceń privacy/security impact,
- [ ] zdecyduj: monitorowanie, halt rollout, rollback lub hotfix,
- [ ] zapisz dowody i follow-up actions.
