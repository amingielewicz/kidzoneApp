# Versioning

## Cel

Dokument opisuje zasady wersjonowania KidZone.

## Schemat

KidZone używa SemVer:

```text
MAJOR.MINOR.PATCH
```

Przykłady:

```text
0.4.0
0.4.1
0.5.0
1.0.0
```

## MAJOR

Zwiększamy, gdy:

- zmienia się główny model działania aplikacji,
- występuje migracja niekompatybilna wstecz,
- zmienia się krytyczna struktura danych,
- release oznacza dużą publiczną wersję produktu.

Przykład:

```text
1.0.0 -> 2.0.0
```

## MINOR

Zwiększamy, gdy:

- dodajemy nową funkcję,
- dodajemy nowy ekran,
- istotnie zmieniamy UX,
- dodajemy większą integrację,
- zmieniamy proces produktu bez łamania kompatybilności.

Przykład:

```text
0.4.0 -> 0.5.0
```

## PATCH

Zwiększamy, gdy:

- naprawiamy błąd,
- robimy hotfix,
- poprawiamy drobny UX,
- poprawiamy dokumentację release,
- wprowadzamy małą zmianę bez wpływu na zakres funkcjonalny.

Przykład:

```text
0.5.0 -> 0.5.1
```

## Pre-release

Dopuszczalne oznaczenia:

```text
0.5.0-alpha.1
0.5.0-beta.1
0.5.0-rc.1
```

## Release candidate

RC oznacza wersję kandydacką do wydania.

Wersja RC może iść do Internal Testing lub Closed Testing, ale publiczny rollout wymaga przejścia GO / NO-GO checklist.

## Hotfix

Hotfix zawsze zwiększa PATCH.

Przykład:

```text
0.5.0 -> 0.5.1
```

## Checklist wersji

- [ ] Version name ustawiony poprawnie.
- [ ] Version code zwiększony.
- [ ] Release notes przygotowane.
- [ ] GO / NO-GO checklist wykonana.
- [ ] Tag Git przygotowany po merge.
