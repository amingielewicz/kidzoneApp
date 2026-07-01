# Observability

## Cel

Dokument opisuje standard obserwowalnosci aplikacji KidZone: logi, crash reporting, metryki i diagnostyke.

## Zasady ogolne

- Diagnostyka ma pomagac znalezc przyczyne problemu.
- Dane wrazliwe nie moga trafiac do logow ani raportow diagnostycznych.
- Komunikat dla uzytkownika jest oddzielony od komunikatu technicznego.
- Bledy krytyczne powinny byc widoczne w Crashlytics.

## Poziomy zdarzen

```text
DEBUG   informacje developerskie
INFO    istotne zdarzenia aplikacyjne
WARN    nietypowa, ale obsluzona sytuacja
ERROR   blad wymagajacy diagnozy
```

## Co warto zbierac

- nazwe feature,
- nazwe ekranu,
- nazwe operacji,
- wersje aplikacji,
- srodowisko,
- typ bledu,
- status operacji.

## Czego nie zbierac

- sekretow,
- pelnych danych prywatnych,
- pelnych payloadow uzytkownika,
- precyzyjnej lokalizacji bez potrzeby,
- danych osobowych w tresci zdarzenia.

## Crashlytics

Do Crashlytics trafiaja:

- crashe,
- bledy krytyczne,
- bledy trudne do odtworzenia,
- bezpieczny kontekst diagnostyczny.

## Checklist

- [ ] Zdarzenie nie zawiera danych wrazliwych.
- [ ] Blad krytyczny trafia do Crashlytics.
- [ ] Zdarzenie ma jasny kontekst.
- [ ] Diagnostyka nie spamuje podczas normalnego uzycia aplikacji.
- [ ] Komunikat uzytkownika jest oddzielony od komunikatu technicznego.
