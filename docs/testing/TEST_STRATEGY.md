# Test Strategy

Ostatnia aktualizacja: 2026-07-14

## Cel

Strategia testowania kidZone określa odpowiedzialność poszczególnych poziomów testów, minimalne bramki jakości oraz zakres wymagany przed release.

## Poziomy testów

### Unit

Obejmują:

- use case'y,
- ViewModel,
- mappery,
- walidatory,
- sortowanie, filtrowanie i limity,
- retry, idempotencję i offline state,
- mapowanie błędów,
- logikę runtime permissions,
- cleanup helpers dla account deletion.

### Integration

Obejmują:

- repository z Room i Firebase Emulator,
- Firestore Rules i Storage Rules,
- migracje danych,
- Cloud Functions,
- synchronizację cache,
- agregaty i eventy backendowe.

### UI i instrumentation

Obejmują stabilne, krytyczne flow:

- auth,
- główną nawigację,
- formularze,
- runtime permissions,
- mapę i fallback listowy,
- deep linki,
- logout i account deletion.

### Manual exploratory

Obejmują:

- UX,
- TalkBack i dużą czcionkę,
- różne urządzenia i wersje Androida,
- offline i słabą sieć,
- uprawnienia i ustawienia systemowe,
- powiadomienia,
- wydajność i koszty,
- zachowanie release builda.

### Release smoke

Minimalny zestaw wykonywany na signed buildzie przed zwiększeniem rollout.

## Zasada ryzyka

Zakres testów rośnie, gdy zmiana dotyka:

- danych użytkownika,
- auth lub account deletion,
- Firestore/Storage Rules,
- mapy i lokalizacji,
- uploadu zdjęć,
- nawigacji i deep linków,
- migracji,
- billing lub kosztów Firebase/Maps,
- kodu współdzielonego przez wiele ekranów.

## Bramki PR

- unit tests dla zmienionej logiki,
- test regresji dla naprawianego błędu,
- Rules tests dla zmian Firestore/Storage,
- build i lint dla Cloud Functions i panelu admina,
- aktualizacja dokumentacji i checklist, jeśli zmiana wpływa na release, privacy lub security,
- brak prawdziwych sekretów i produkcyjnych danych testowych.

## Definition of Done

- kryteria akceptacji są pokryte,
- scenariusze negatywne są sprawdzone,
- retry i double submit są zweryfikowane,
- błędy są obserwowalne bez PII,
- accessibility została sprawdzona dla zmiany UI,
- brak nieudokumentowanej luki testowej,
- CI przechodzi,
- dowody testów są dostępne w PR lub release record.

## Scenariusze negatywne

- brak internetu i timeout,
- wygasła sesja,
- zwykła i trwała odmowa zgody,
- wyłączony GPS,
- nieprawidłowy deep link,
- błąd Rules lub App Check,
- częściowy upload,
- częściowy cleanup konta,
- równoległe requesty,
- stary build podczas migracji,
- nieważny token FCM.

## Flaky tests

- flaky test nie jest bezterminowo ignorowany,
- wymaga issue, właściciela i opisu wpływu,
- ponowne uruchomienie nie zastępuje diagnozy,
- krytyczna bramka flaky oznacza NO-GO, dopóki ryzyko nie zostanie zaakceptowane.

## Checklista

- [ ] właściwy poziom testów został wybrany,
- [ ] testy pozytywne i negatywne przechodzą,
- [ ] regresja ma test odtwarzający,
- [ ] zmiany danych mają test migracji,
- [ ] UI ma sprawdzenie accessibility,
- [ ] signed build ma smoke PASS,
- [ ] dokumentacja wyników jest kompletna.
