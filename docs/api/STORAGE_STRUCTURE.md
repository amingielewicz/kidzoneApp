# Firebase Storage Structure

## Cel

Dokument opisuje organizację plików w Firebase Storage oraz zasady uploadu, bezpieczeństwa i utrzymania.

## Struktura katalogów

```text
places/
  {placeId}/
    photos/

users/
  {userId}/
    avatar/

reports/
  {reportId}/
    attachments/
```

## Zasady nazewnictwa

- Nazwy plików powinny być unikalne.
- Preferowane są identyfikatory UUID.
- Nie używamy nazw dostarczonych przez użytkownika jako ścieżek.

## Upload

- Dozwolone formaty obrazów.
- Walidacja typu MIME.
- Walidacja maksymalnego rozmiaru pliku.
- Kompresja po stronie aplikacji przed wysłaniem.
- Usuwanie metadanych lokalizacyjnych zgodnie z polityką prywatności.

## Security Rules

- Użytkownik może dodawać tylko własne pliki.
- Odczyt zależy od rodzaju zasobu.
- Operacje administracyjne wymagają odpowiednich uprawnień.
- Nie przechowujemy plików wykonywalnych ani nieobsługiwanych formatów.

## Retencja

- Usunięcie miejsca powinno usuwać powiązane pliki.
- Usunięcie użytkownika powinno usuwać dane zgodnie z polityką produktu.
- Osierocone pliki powinny być okresowo wykrywane i czyszczone.

## Monitoring

- Monitorować liczbę uploadów.
- Monitorować wykorzystanie przestrzeni.
- Monitorować błędy uploadu.
- Monitorować koszty transferu.

## Checklist

- [ ] Struktura katalogów jest spójna.
- [ ] Rules chronią wszystkie ścieżki.
- [ ] Limity rozmiaru są ustawione.
- [ ] MIME type jest walidowany.
- [ ] Pliki są usuwane wraz z danymi biznesowymi.
