# Contributing

## Cel zasad

Ten dokument opisuje prosty, darmowy workflow pracy z repozytorium KidZone.

Repozytorium jest prywatne i należy do konta indywidualnego GitHub. Część mechanizmów automatycznej ochrony brancha `main`, takich jak egzekwowane rulesety dla prywatnych repozytoriów, może wymagać płatnego planu GitHub Team lub organizacji. Z tego powodu stosujemy manualną zasadę jakości: **nie mergujemy kodu bez zielonego CI**.

## Główna zasada

Nie commitujemy bezpośrednio do `main`.

Każda zmiana powinna przechodzić przez pull request.

## Branch naming

Stosujemy krótkie, czytelne prefiksy:

| Typ zmiany | Prefix | Przykład |
|---|---|---|
| Nowa funkcja | `feature/` | `feature/add-place-details` |
| Poprawka błędu | `bugfix/` | `bugfix/fix-map-marker` |
| Pilna poprawka | `hotfix/` | `hotfix/fix-crash-on-startup` |
| Testy | `test/` | `test/login-screen-validation` |
| CI/CD | `ci/` | `ci/harden-android-workflow` |
| Dokumentacja | `docs/` | `docs/add-ci-workflow-rules` |
| Porządki | `chore/` | `chore/update-dependencies` |

## Pull request checklist

Przed mergem PR sprawdzamy:

- [ ] PR ma jasny tytuł.
- [ ] PR opisuje, co zostało zmienione.
- [ ] PR opisuje, jak sprawdzić zmianę.
- [ ] `Android CI` jest zielony.
- [ ] Nie ma przypadkowo dodanych sekretów, kluczy API ani plików lokalnych.
- [ ] Nie ma zmian niezwiązanych z celem PR.

## Wymagane CI przed merge

PR można mergować tylko wtedy, gdy workflow `Android CI` jest zielony.

Aktualnie `Android CI` wykonuje między innymi:

- Android Lint,
- unit testy,
- build debug APK,
- skan sekretów przez Gitleaks,
- upload raportów i APK jako artifacty.

Jeżeli `Android CI` jest czerwony, PR nie powinien być mergowany.

## Czego nie mergujemy

Nie mergujemy PR, jeśli:

- build nie przechodzi,
- testy są czerwone,
- lint zgłasza błędy,
- Gitleaks wykrył potencjalny sekret,
- zmiana dodaje prawdziwe klucze API, tokeny albo dane dostępowe,
- nie wiadomo, co zmiana właściwie robi.

## Koszty i narzędzia płatne

Przed dodaniem narzędzia, które może generować koszty, najpierw sprawdzamy jego model płatności.

Przykłady narzędzi lub funkcji, które mogą wymagać dodatkowej weryfikacji kosztów:

- GitHub Advanced Security,
- CodeQL dla prywatnych repozytoriów,
- GitHub Team rulesets dla prywatnych repozytoriów,
- Snyk w wyższych limitach,
- długie testy emulatorowe zużywające minuty GitHub Actions.

Narzędzia aktualnie używane w darmowym zakresie:

- GitHub Actions w ramach dostępnych minut,
- Gitleaks CLI,
- Android Lint,
- Gradle unit tests.

## Manualna ochrona `main`

Ponieważ egzekwowane rulesety dla prywatnego repo mogą wymagać płatnego planu, obowiązuje manualna zasada:

1. Tworzymy branch roboczy.
2. Otwieramy pull request do `main`.
3. Czekamy na wynik `Android CI`.
4. Jeśli `Android CI` jest zielony, można mergować.
5. Jeśli `Android CI` jest czerwony, najpierw naprawiamy problem.

## Zalecany merge method

Preferowany sposób merge:

```text
Squash and merge
```

Dzięki temu historia `main` zostaje czysta i czytelna.
