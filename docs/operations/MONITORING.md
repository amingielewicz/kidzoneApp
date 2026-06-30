# Monitoring Handbook

## Cel

Dokument opisuje monitoring aplikacji KidZone po stronie Androida, Firebase oraz procesu release.

## Narzędzia

- Firebase Crashlytics.
- Firebase Performance Monitoring.
- Firebase Analytics.
- Remote Config.
- Google Play Console.
- GitHub Actions.

## Crashlytics

Crashlytics powinien odpowiadać na pytania:

- czy aplikacja crashuje po release,
- gdzie crash występuje,
- jakiej wersji dotyczy,
- czy problem rośnie po rollout,
- czy hotfix rozwiązał problem.

## Performance Monitoring

Trace powinny obejmować:

- cold start,
- load nearby places,
- load map places,
- load ranking,
- search places,
- add place,
- upload place photo.

## Analytics

Analytics powinien mierzyć zachowania produktowe, nie dane wrażliwe.

Przykłady eventów:

- onboarding completed,
- place searched,
- place opened,
- place added,
- review added,
- ranking opened,
- map marker selected.

Nie wysyłamy surowych tekstów użytkownika, e-maili, tokenów ani dokładnej lokalizacji.

## Remote Config

Parametry warte monitorowania i kontroli:

- map marker limit,
- list page size,
- ranking limit,
- search debounce,
- map debounce,
- maintenance mode,
- feature flags.

## Alerty

Alert powinien powstać dla:

- wzrostu crash rate,
- wzrostu ANR,
- problemów z logowaniem,
- problemów z mapą,
- nietypowego wzrostu kosztów Firebase / Maps,
- błędów Cloud Functions.

## Po release

Po każdym release sprawdzić:

- [ ] Crashlytics po 1 godzinie.
- [ ] Crashlytics po 24 godzinach.
- [ ] Performance po 24 godzinach.
- [ ] Play Console vitals.
- [ ] Firebase usage / billing.
- [ ] Zgłoszenia użytkowników.

## Incident checklist

- [ ] Ustalić wersję aplikacji.
- [ ] Ustalić zakres użytkowników.
- [ ] Sprawdzić Crashlytics.
- [ ] Sprawdzić Firebase usage.
- [ ] Sprawdzić ostatnie PR-y.
- [ ] Zdecydować: rollback, hotfix albo monitorowanie.
