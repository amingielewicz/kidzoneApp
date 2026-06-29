# KidZone Engineering Handbook

Ten katalog jest źródłem prawdy dla zasad technicznych, produktowych i release'owych aplikacji KidZone.

## Struktura

| Obszar | Dokument |
|---|---|
| Release readiness | [`release/GO_NO_GO_CHECKLIST.md`](release/GO_NO_GO_CHECKLIST.md) |
| Proces release | [`release/RELEASE_PROCESS.md`](release/RELEASE_PROCESS.md) |
| Release notes | [`release/RELEASE_NOTES_TEMPLATE.md`](release/RELEASE_NOTES_TEMPLATE.md) |
| Development | [`development/CODING_GUIDELINES.md`](development/CODING_GUIDELINES.md) |
| UI / UX | [`development/UI_UX_GUIDELINES.md`](development/UI_UX_GUIDELINES.md) |
| Performance / Security / QA | [`development/PERFORMANCE_SECURITY_TESTING.md`](development/PERFORMANCE_SECURITY_TESTING.md) |

## Zasada główna

Dokumentacja w repo ma być praktyczna. Każdy dokument powinien odpowiadać na pytanie:

> Co trzeba zrobić, żeby aplikacja była stabilna, bezpieczna, czytelna i gotowa do release?

## Poziomy gotowości produktu

### MVP / Release Candidate (80–85%)

Minimalny poziom pozwalający rozpocząć Internal Testing lub Closed Testing.

### Optimum / Produkt Premium (90–95%)

Poziom rekomendowany dla pierwszego publicznego release.

### Enterprise / Maximum (98–100%)

Poziom docelowy dla produktu rozwijanego długoterminowo przez większy zespół.

## Kiedy aktualizować dokumentację

- przy zmianie procesu release,
- przy zmianie architektury,
- przy zmianie standardów UI/UX,
- przy dodaniu nowego krytycznego flow,
- po istotnym incydencie produkcyjnym,
- po zmianach w bezpieczeństwie lub Firebase Rules.
