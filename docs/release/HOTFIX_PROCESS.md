# Hotfix Process

## Cel

Hotfix to szybka poprawka błędu produkcyjnego lub release candidate, która nie powinna czekać na standardowy cykl developmentu.

## Kiedy robimy hotfix

Hotfix jest uzasadniony, gdy:

- aplikacja crashuje po starcie,
- logowanie lub rejestracja nie działa,
- mapa albo lista miejsc nie działa,
- dodawanie miejsca nie działa,
- występuje problem security lub privacy,
- release build ma krytyczną regresję,
- Firebase Rules albo App Check blokują prawidłowe użycie aplikacji.

## Czego nie robimy jako hotfix

Hotfix nie służy do:

- dodawania nowych funkcji,
- większych refaktorów,
- zmian design systemu,
- przebudowy architektury,
- poprawek kosmetycznych bez wpływu na release.

## Branch

Branch hotfix powinien wychodzić z aktualnego stabilnego refa:

```text
hotfix/x.y.z-short-description
```

Przykład:

```text
hotfix/0.5.1-login-crash
```

## Proces

```text
production tag / main
  -> hotfix branch
  -> minimal fix
  -> review
  -> smoke test
  -> signed build
  -> release
  -> monitor
```

## Minimalne wymagania

- [ ] Problem jest potwierdzony.
- [ ] Zakres jest minimalny.
- [ ] Nie dodano nowej funkcjonalności.
- [ ] Fix ma code review.
- [ ] Naprawiany flow został przetestowany.
- [ ] Release build przechodzi.
- [ ] Release notes zostały zaktualizowane.

## Testy hotfixa

Wymagane:

- [ ] test naprawianego flow,
- [ ] smoke test aplikacji,
- [ ] test czystej instalacji,
- [ ] test aktualizacji z poprzedniej wersji, jeśli dotyczy,
- [ ] sprawdzenie Crashlytics po publikacji.

## Versioning

Hotfix zwiększa PATCH:

```text
0.5.0 -> 0.5.1
```

Nie zwiększamy MINOR dla hotfixa.

## Monitoring po hotfixie

Po publikacji sprawdzić:

- [ ] Crashlytics po 1 godzinie,
- [ ] Crashlytics po 24 godzinach,
- [ ] Google Play Console vitals,
- [ ] Firebase usage,
- [ ] zgłoszenia użytkowników.

## Checklist zamknięcia

- [ ] Hotfix wdrożony.
- [ ] Problem nie występuje w monitoringu.
- [ ] Release notes uzupełnione.
- [ ] Follow-up issue utworzone, jeśli potrzebny jest większy refactor.
- [ ] Postmortem zapisane dla P0/P1.
