# Testy manualne: lokalizacja i sieć

## AddPlace / offline

### Wejście do AddPlace bez internetu

**Warunki początkowe:**
- Wi-Fi wyłączone.
- Dane komórkowe wyłączone.
- VPN wyłączony, jeśli jest używany.

**Kroki:**
1. Otwórz aplikację.
2. Przejdź do ekranu `AddPlace`.
3. Sprawdź dolny przycisk zapisu.

**Oczekiwany rezultat:**
- Przycisk pokazuje tekst `Zapisz po odzyskaniu internetu`.
- Przycisk jest wyszarzony i nieaktywny.
- Pod przyciskiem widoczny jest krótki tekst `Brak internetu`.
- Nie pojawia się czarny Snackbar offline.
- Nie uruchamia się upload zdjęć.
- Nie pojawia się spinner zapisu.

### Utrata internetu podczas pracy na AddPlace

**Warunki początkowe:**
- Internet jest dostępny.
- Użytkownik znajduje się na ekranie `AddPlace`.

**Kroki:**
1. Wypełnij część formularza.
2. Wyłącz Wi-Fi i dane komórkowe albo włącz tryb samolotowy.
3. Obserwuj dolny przycisk zapisu.

**Oczekiwany rezultat:**
- Aplikacja wykrywa utratę internetu bez restartu ekranu.
- Przycisk zmienia tekst na `Zapisz po odzyskaniu internetu`.
- Przycisk staje się wyszarzony.
- Pod przyciskiem widoczny jest tekst `Brak internetu`.
- Formularz nie jest czyszczony.
- Nie pojawia się czarny Snackbar offline.

### Odzyskanie internetu podczas pracy na AddPlace

**Warunki początkowe:**
- Użytkownik znajduje się na ekranie `AddPlace`.
- Internet jest wyłączony.
- Przycisk pokazuje `Zapisz po odzyskaniu internetu`.

**Kroki:**
1. Włącz Wi-Fi lub dane komórkowe.
2. Poczekaj na odzyskanie połączenia.
3. Sprawdź dolny przycisk zapisu.

**Oczekiwany rezultat:**
- Przycisk wraca do tekstu `Zapisz miejsce` albo `Aktualizuj miejsce` w trybie edycji.
- Przycisk jest aktywny, jeśli formularz jest poprawnie wypełniony.
- Tekst `Brak internetu` znika.

## GPS / ekran Start

### GPS OFF przy wejściu na ekran

**Warunki początkowe:**
- GPS/lokalizacja systemowa jest wyłączona.
- Aplikacja uruchamia się na ekranie Start.

**Kroki:**
1. Otwórz aplikację.
2. Przejdź na ekran Start.
3. Sprawdź nagłówek aplikacji.
4. Sprawdź sekcję lokalizacji na ekranie Start.

**Oczekiwany rezultat:**
- W nagłówku widoczna jest przekreślona ikona GPS.
- Widoczny jest komunikat zachęcający do włączenia GPS.
- Widoczny jest przycisk `Włącz GPS`.
- Aplikacja nie crashuje.
- Aplikacja nie pokazuje fałszywie atrakcji w pobliżu.

### Włączenie GPS z poziomu aplikacji

**Warunki początkowe:**
- GPS/lokalizacja systemowa jest wyłączona.
- Użytkownik znajduje się na ekranie Start.
- Widoczny jest przycisk `Włącz GPS`.

**Kroki:**
1. Kliknij `Włącz GPS`.
2. W ustawieniach systemowych włącz lokalizację/GPS.
3. Wróć do aplikacji.
4. Sprawdź ikonę GPS w nagłówku.
5. Sprawdź banner/komunikat lokalizacji.

**Oczekiwany rezultat:**
- Po powrocie do aplikacji stan GPS zostaje odświeżony.
- Przekreślona ikona GPS znika.
- Banner `Włącz GPS` nie zostaje na ekranie, jeśli GPS jest już włączony.
- Aplikacja nie wymaga restartu, aby odświeżyć stan GPS.

### Wyłączenie GPS podczas korzystania z aplikacji

**Warunki początkowe:**
- GPS/lokalizacja systemowa jest włączona.
- Użytkownik znajduje się na ekranie Start.

**Kroki:**
1. Będąc na ekranie Start, wyłącz GPS/lokalizację w systemie.
2. Wróć do aplikacji, jeśli została przeniesiona w tło.
3. Sprawdź nagłówek aplikacji.
4. Sprawdź sekcję lokalizacji.

**Oczekiwany rezultat:**
- Aplikacja wykrywa wyłączenie GPS.
- W nagłówku pojawia się przekreślona ikona GPS.
- Na ekranie pojawia się komunikat/przycisk `Włącz GPS`.
- Aplikacja nie pokazuje mylącego stanu, że GPS nadal działa.

### Ponowne włączenie GPS bez restartu aplikacji

**Warunki początkowe:**
- Aplikacja jest uruchomiona.
- GPS został wcześniej wyłączony.
- Widoczna jest przekreślona ikona GPS.

**Kroki:**
1. Włącz GPS/lokalizację w systemie.
2. Wróć do aplikacji.
3. Obserwuj nagłówek i sekcję lokalizacji.

**Oczekiwany rezultat:**
- Stan GPS aktualizuje się automatycznie.
- Przekreślona ikona GPS znika.
- Komunikat `Włącz GPS` nie zostaje widoczny, gdy GPS jest już aktywny.
- Nie trzeba ubijać ani restartować aplikacji.
