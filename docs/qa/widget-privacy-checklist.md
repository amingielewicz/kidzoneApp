# Widget privacy checklist

Powiązane issue: #294

Ta checklista służy do ręcznej weryfikacji `NearbyPlacesWidget` przed release. Widget pokazuje publiczne miejsca z lokalnego Room cache i korzysta z ostatniej lokalizacji zapisanej w prywatnych `SharedPreferences`.

## Kontekst bezpieczeństwa

- Widget nie powinien pokazywać prywatnych danych użytkownika.
- Po logout/delete aplikacja czyści ostatnią lokalizację widgetu i wymusza odświeżenie widgetu.
- Room cache miejsc zawiera publiczne dane miejsc. Jeżeli w release uznamy, że po logout/delete nie wolno pokazywać nawet publicznego cache, trzeba dodać osobne issue na czyszczenie Room cache.
- Receiver widgetu jest eksportowany, bo wymaga tego AppWidget. Manualnie trzeba sprawdzić, czy update nie powoduje crashy ani nadmiernego odświeżania.

## Przygotowanie

Zanotuj:

```text
Build:
Device:
Android version:
Account type: email/password / Google
Widget added to home screen: yes / no
Lock screen widgets tested: yes / no / not supported
```

## Scenariusze

| Scenariusz | Oczekiwany wynik | Status | Dowód |
| --- | --- | --- | --- |
| Widget bez cache miejsc | Pokazuje pusty stan, brak crasha. | Do sprawdzenia | |
| Widget bez zapisanej lokalizacji | Pokazuje fallback bez dystansu albo pusty stan, brak crasha. | Do sprawdzenia | |
| Widget po pobraniu lokalizacji | Pokazuje maksymalnie 3 miejsca i dystans. | Do sprawdzenia | |
| Logout | Ostatnia lokalizacja widgetu jest czyszczona, widget odświeża się, brak prywatnych danych. | Do sprawdzenia | |
| Ponowny login innym kontem | Widget nie ujawnia lokalizacji poprzedniego użytkownika. | Do sprawdzenia | |
| Delete account | Ostatnia lokalizacja widgetu jest czyszczona, widget odświeża się, brak prywatnych danych. | Do sprawdzenia | |
| Brak uprawnienia lokalizacji | Aplikacja i widget nie crashują. | Do sprawdzenia | |
| Ekran blokady / podgląd widgetu | Treść widgetu jest akceptowalna dla release. | Do sprawdzenia | |
| Wymuszony update widgetu | Brak crasha i brak pętli odświeżania. | Do sprawdzenia | |

## Komendy pomocnicze

Lista widgetów / broadcastów zależy od urządzenia i launchera, ale przydatne są:

```powershell
adb logcat | Select-String -Pattern "NearbyPlacesWidget|Glance|AppWidget|KidZone"
```

```powershell
adb shell cmd appwidget list
```

## Wynik

```text
Widget privacy result: PASS / FAIL / BLOCKED
Known risks:
Follow-up issues:
Evidence:
```

Issue #294 można zamknąć dopiero po wpisaniu wyniku ręcznej weryfikacji.
