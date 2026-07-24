# Hotfix Process

Ostatnia aktualizacja: 2026-07-14

## Cel

Hotfix to minimalna poprawka krytycznego problemu produkcyjnego lub release candidate. Nie zastępuje standardowego procesu rozwoju i nie służy do dokładania nowych funkcji.

## Kiedy hotfix jest uzasadniony

- crash przy starcie,
- niedziałające logowanie, rejestracja lub sesja,
- niedostępna Mapa, Lista lub podstawowe dodawanie treści,
- regresja runtime permissions,
- niedziałające account deletion,
- problem security lub privacy,
- Firebase Rules, App Check albo konfiguracja blokują prawidłowy build,
- istotny wzrost crash rate, ANR lub kosztów,
- krytyczny błąd deep linków, powiadomień albo migracji.

## Czego nie robimy jako hotfix

- nowych funkcji,
- dużego refaktoru,
- przebudowy architektury,
- zmian design systemu,
- szerokiego porządkowania kodu,
- kosmetyki bez wpływu na bezpieczeństwo lub używalność.

## Źródło brancha

Branch wychodzi z dokładnego stabilnego refa używanego przez produkcję:

```text
hotfix/x.y.z-short-description
```

Przykład:

```text
hotfix/0.5.1-login-crash
```

Nie zakładamy brancha z developmentu zawierającego niewydane zmiany.

## Proces

```text
incident confirmed
  → halt rollout / mitigation
  → branch from production ref
  → minimal fix
  → regression test
  → review
  → signed build
  → smoke
  → release
  → monitor
  → back-merge fix
```

## Ograniczenie skutków przed kodem

Przed przygotowaniem hotfixa rozważ:

- zatrzymanie staged rollout,
- rollback Remote Config,
- wyłączenie funkcji feature flagem,
- przywrócenie poprzednich Rules lub konfiguracji,
- czasowe ograniczenie kosztownej funkcji,
- komunikat maintenance.

Działanie ograniczające nie może pogorszyć bezpieczeństwa lub utraty danych.

## Minimalne wymagania

- [ ] problem jest potwierdzony i ma priorytet,
- [ ] znany jest dotknięty build i track,
- [ ] zakres poprawki jest minimalny,
- [ ] istnieje test odtwarzający błąd, jeśli technicznie możliwe,
- [ ] fix ma review,
- [ ] nie dodano niepowiązanej funkcjonalności,
- [ ] ryzyko privacy/security zostało ocenione,
- [ ] `VERSION_CODE` został zwiększony,
- [ ] release notes zostały zaktualizowane.

## Testy hotfixa

Obowiązkowo:

- naprawiany flow,
- test regresji obszaru współdzielonego,
- release smoke,
- czysta instalacja,
- aktualizacja z poprzedniego builda, jeśli dotyczy,
- brak internetu lub scenariusz błędu związany z incydentem,
- signed build z produkcyjną konfiguracją.

Dodatkowo według zakresu:

- Firestore/Storage Rules tests,
- App Check smoke,
- runtime permissions matrix,
- account deletion checklist,
- deep link/FCM,
- migracja Room lub Firestore,
- performance i koszt.

## Versioning

Hotfix zwykle zwiększa PATCH i zawsze zwiększa `VERSION_CODE`:

```text
0.5.0 → 0.5.1
```

Nie zmieniamy MINOR, chyba że obecny model wersjonowania projektu wymaga innej, jawnie udokumentowanej decyzji.

## Publikacja

- użyj Internal Testing lub wybranego bezpiecznego tracku do smoke,
- nie pomijaj signed AAB,
- zastosuj staged rollout, jeśli czas i skala incydentu na to pozwalają,
- zatrzymaj rollout przy nowym P0/P1,
- zapisz decyzję GO / NO-GO i dowody.

## Monitoring

Po publikacji sprawdź:

- Crashlytics i Android vitals,
- naprawiany wskaźnik lub flow,
- błędy logowania, Rules i App Check,
- account deletion lub permissions, jeśli dotyczy,
- Firebase/Maps usage i koszty,
- opinie i zgłoszenia użytkowników.

Kontrola powinna nastąpić bezpośrednio po rozpoczęciu rollout oraz ponownie po zebraniu reprezentatywnych danych.

## Synchronizacja gałęzi

Po wydaniu poprawka musi trafić również do aktywnej gałęzi rozwojowej. Nie kopiujemy zmian ręcznie bez historii, jeśli można wykonać merge albo cherry-pick kontrolowanego commita.

## Zamknięcie

- [ ] hotfix został wdrożony,
- [ ] monitoring potwierdza poprawę,
- [ ] rollout ma końcową decyzję,
- [ ] poprawka trafiła do aktywnego developmentu,
- [ ] release notes i dokumentacja są aktualne,
- [ ] follow-up issue istnieje dla szerszej naprawy,
- [ ] postmortem zapisano dla P0/P1,
- [ ] dodano test lub kontrolę zapobiegającą regresji.
