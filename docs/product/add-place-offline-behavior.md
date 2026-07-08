# AddPlace — zachowanie offline

## Cel

Użytkownik nie może stracić danych wpisanych w formularzu `AddPlace`, gdy w trakcie dodawania miejsca zniknie internet albo GPS.

## Etap 1 — bezpieczny UX bez utraty danych

Zakres MVP:

- formularz nie jest czyszczony po błędzie sieci,
- brak internetu jest sygnalizowany ikoną `SignalWifiOff` w nagłówku,
- brak GPS albo brak uprawnień lokalizacji jest sygnalizowany ikoną `GpsOff` w nagłówku,
- kliknięcie ikony GPS uruchamia istniejący flow uprawnień / ustawień lokalizacji,
- kliknięcie ikony internetu pokazuje krótką informację, że trzeba sprawdzić połączenie,
- przy błędzie zapisu formularz zostaje na ekranie i użytkownik może ponowić zapis.

## Etap 2 — lokalny szkic formularza

Docelowe zachowanie:

- `AddPlace` automatycznie zapisuje stan formularza lokalnie po zmianie pól,
- szkic obejmuje:
  - nazwę,
  - opis,
  - kategorię,
  - adres,
  - współrzędne,
  - udogodnienia,
  - lokalne URI zdjęć,
- po ponownym wejściu w `AddPlace` aplikacja proponuje przywrócenie szkicu,
- po udanym zapisie szkic jest usuwany.

Preferowane miejsce techniczne:

- Room, jeśli szkic ma być rozwijany o kolejkę i historię,
- DataStore/SharedPreferences tylko dla prostego pojedynczego szkicu MVP.

## Etap 3 — kolejka wysyłki

Projekt ma już fundament:

- `PendingOperationEntity`,
- `PendingOperationDao`,
- `SyncManager`,
- `SyncWorker`,
- WorkManager z warunkiem `NetworkType.CONNECTED`.

Warunek wdrożenia:

- `SyncWorker` musi mieć produkcyjny processor dla `OperationType.ADD_PLACE`, który faktycznie zapisuje miejsce do Firestore,
- payload musi zachowywać pełne dane miejsca,
- zdjęcia muszą być obsłużone osobno, bo upload do Firebase Storage wymaga sieci przed zapisem URL-i.

## Etap 4 — pełny offline-first

Docelowo:

- przy braku internetu przycisk może zmieniać tekst na `Wyślij, gdy będziesz online`,
- kliknięcie zapisuje miejsce do kolejki,
- WorkManager wysyła je po odzyskaniu internetu,
- użytkownik dostaje potwierdzenie po synchronizacji,
- w przypadku błędu operacja trafia do retry/dead-letter.

## Ważne ograniczenie

Nie należy udawać sukcesu zapisu online, jeśli operacja została tylko zakolejkowana. UI powinien rozróżniać:

- `zapisano na serwerze`,
- `zapisano lokalnie i wyślemy później`,
- `zapis nieudany`.
