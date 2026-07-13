# Widget privacy checklist

Powiązane issue: #294

Ostatnia aktualizacja: 2026-07-14

## Cel

Manualna weryfikacja prywatności i stabilności `NearbyPlacesWidget` przed release. Widget korzysta z publicznego cache miejsc w Room oraz prywatnego stanu ostatniej lokalizacji.

## Zasady bezpieczeństwa

- widget nie pokazuje danych profilu, e-maila ani treści prywatnych,
- ostatnia lokalizacja jest traktowana jako dane prywatne,
- logout, ban sign-out i account deletion czyszczą lokalizację widgetu,
- po cleanup widget jest odświeżany,
- publiczny cache miejsc może pozostać tylko wtedy, gdy nie ujawnia poprzedniego użytkownika,
- receiver nie może powodować pętli odświeżeń ani crashy po zewnętrznym update.

## Dane testu

```text
Build:
Commit:
Device:
Android:
Launcher:
Account type:
Widget on home screen: yes / no
Lock screen widget: yes / no / unsupported
Result: PASS / FAIL / BLOCKED
```

## Przygotowanie

- [ ] dodaj widget do ekranu głównego,
- [ ] sprawdź stan bez uruchamiania aplikacji,
- [ ] uruchom aplikację online i wypełnij cache,
- [ ] użyj lokalizacji, jeśli scenariusz tego wymaga,
- [ ] przygotuj drugie konto testowe,
- [ ] włącz logcat dla widgetu i Glance.

## Scenariusze

| Scenariusz | Oczekiwany wynik |
| --- | --- |
| brak cache miejsc | empty state, brak crasha |
| brak zapisanej lokalizacji | fallback bez dystansu albo czytelny pusty stan |
| lokalizacja dostępna | maksymalna oczekiwana liczba miejsc i poprawny dystans |
| brak zgody lokalizacji | aplikacja i widget nie crashują |
| GPS wyłączony | kontrolowany fallback |
| offline z cache | widget pokazuje bezpieczny cache |
| offline bez cache | pusty stan, brak nieskończonego loadingu |
| logout | prywatna lokalizacja wyczyszczona, widget odświeżony |
| login innym kontem | brak lokalizacji poprzedniego użytkownika |
| ban sign-out | ten sam cleanup co przy logout |
| account deletion | pełny cleanup prywatnego stanu |
| restart urządzenia | widget nie ujawnia starego prywatnego stanu |
| update aplikacji | widget nadal działa albo pokazuje kontrolowany fallback |
| wymuszony update | brak crasha i pętli |
| szybka seria update | brak lawiny requestów i ANR |
| ekran blokady / podgląd | treść jest akceptowalna prywatnościowo |

## Dostępność

- [ ] nazwa widgetu jest czytelna,
- [ ] miejsca i akcje mają zrozumiałe etykiety,
- [ ] duża czcionka nie ucina kluczowych danych,
- [ ] kolor nie jest jedynym nośnikiem informacji,
- [ ] kliknięcie otwiera właściwy ekran lub kontrolowany fallback.

## Dane i cache

- [ ] Room zawiera wyłącznie publiczne dane wymagane przez widget,
- [ ] prywatna lokalizacja nie trafia do publicznego dokumentu ani logu,
- [ ] brak e-maila, UID, tokenu FCM i treści profilu,
- [ ] TTL i odświeżanie nie pozostawiają oczywiście nieaktualnych danych,
- [ ] usunięte lub zablokowane miejsce znika po odświeżeniu,
- [ ] account deletion nie pozostawia pending operation powiązanej z kontem.

## Stabilność i koszty

- [ ] update ma limit zapytań,
- [ ] brak odświeżania przy każdej recomposition aplikacji,
- [ ] wiele instancji widgetu nie mnoży niekontrolowanie requestów,
- [ ] błąd sieci nie uruchamia agresywnego retry,
- [ ] logi nie zawierają dokładnej lokalizacji ani PII.

## Komendy pomocnicze

```powershell
adb logcat | Select-String -Pattern "NearbyPlacesWidget|Glance|AppWidget|KidZone"
```

```powershell
adb shell cmd appwidget list
```

Dostępność komend zależy od urządzenia i wersji Androida.

## Wynik

```text
Widget privacy: PASS / FAIL / BLOCKED
Accessibility: PASS / FAIL / BLOCKED
Logout cleanup: PASS / FAIL / BLOCKED
Account deletion cleanup: PASS / FAIL / BLOCKED
Performance: PASS / FAIL / BLOCKED
Known risks:
Follow-up issues:
Evidence:
```

Issue #294 można zamknąć dopiero po zapisaniu wyniku testu na release candidate.
