# Versioning

Ostatnia aktualizacja: 2026-07-13

## Cel

Zasady wersjonowania kidZone i powiązania wersji aplikacji z Google Play, tagami Git oraz release notes.

## Źródło wersji

Wersja aplikacji znajduje się w `version.properties`:

```properties
VERSION_NAME=1.0.0
VERSION_CODE=1
```

Build Android odczytuje te wartości podczas kompilacji.

## VERSION_NAME

`VERSION_NAME` używa SemVer:

```text
MAJOR.MINOR.PATCH
```

Przykłady:

```text
0.5.0
0.5.1
1.0.0
1.1.0
```

### MAJOR

Zwiększamy dla dużej, niekompatybilnej zmiany produktu, danych lub API.

```text
1.4.2 → 2.0.0
```

### MINOR

Zwiększamy przy nowej funkcji lub większej zmianie UX bez łamania kompatybilności.

```text
1.4.2 → 1.5.0
```

### PATCH

Zwiększamy przy poprawce błędu, hotfixie albo małej zmianie bez nowej funkcji.

```text
1.4.2 → 1.4.3
```

Sama zmiana dokumentacji bez nowego builda nie wymaga zmiany wersji aplikacji.

## Pre-release

Dopuszczalne oznaczenia:

```text
1.0.0-alpha.1
1.0.0-beta.1
1.0.0-rc.1
```

RC może trafić do Internal lub Closed Testing. Publiczny rollout wymaga finalnej wersji i decyzji GO.

## VERSION_CODE

`VERSION_CODE` jest liczbą całkowitą używaną przez Google Play.

Zasady:

- musi być wyższy przy każdym uploadzie AAB do Google Play,
- nie może zostać ponownie użyty nawet dla odrzuconego lub wycofanego builda,
- może rosnąć niezależnie od `VERSION_NAME`,
- hotfix zawsze dostaje nowy `VERSION_CODE`.

Przykład:

```properties
VERSION_NAME=1.0.0-rc.1
VERSION_CODE=14
```

Kolejny upload:

```properties
VERSION_NAME=1.0.0-rc.2
VERSION_CODE=15
```

## Tag Git

Tag powinien odpowiadać `VERSION_NAME`:

```text
v1.0.0
v1.0.1
v1.1.0
```

Tag tworzymy dla dokładnego commita, z którego powstał podpisany AAB.

## Release notes

Release notes powinny zawierać:

- funkcje widoczne dla użytkownika,
- poprawki błędów,
- ważne zmiany privacy i security,
- znane ograniczenia,
- numer wersji i commit lub tag.

## Hotfix

Hotfix:

- zwiększa PATCH,
- zawsze zwiększa `VERSION_CODE`,
- dostaje osobny tag,
- wymaga testu poprawianego flow i regresji obszaru ryzyka.

## Checklista

- [ ] `VERSION_NAME` ustawiony poprawnie,
- [ ] `VERSION_CODE` wyższy niż ostatni upload,
- [ ] release notes odpowiadają zmianom,
- [ ] signed AAB powstał z zapisanego commita,
- [ ] tag odpowiada `VERSION_NAME`,
- [ ] GO / NO-GO wykonane,
- [ ] numer wersji widoczny w aplikacji odpowiada buildowi.
