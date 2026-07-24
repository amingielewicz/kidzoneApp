# Firebase Storage Structure

Ostatnia aktualizacja: 2026-07-13

## Cel

Dokument opisuje organizację plików w Firebase Storage oraz zasady bezpieczeństwa, uploadu, retencji i cleanup.

## Struktura

```text
places/
  {ownerUserId}/
    {placeId}/
      photos/
        {fileId}

reviews/
  {ownerUserId}/
    {reviewId}/
      photos/
        {fileId}

users/
  {userId}/
    avatar/
      {fileId}

reports/
  {userId}/
    {reportId}/
      attachments/
        {fileId}
```

Ścieżka powinna zawierać właściciela tam, gdzie ułatwia to bezpieczne Rules i cleanup.

## Nazewnictwo

- używamy UUID lub bezpiecznego identyfikatora generowanego przez aplikację,
- nie używamy nazwy pliku od użytkownika jako ścieżki,
- rozszerzenie nie jest źródłem prawdy o typie pliku,
- identyfikator pliku nie zawiera danych osobowych.

## Upload

- Photo Picker działa bez szerokiego dostępu do galerii,
- kamera ma osobne opcjonalne uprawnienie,
- aplikacja waliduje liczbę, rozmiar i typ plików,
- zdjęcia są kompresowane,
- EXIF/GPS są usuwane, jeśli nie są potrzebne,
- upload ma timeout i kontrolowany retry,
- częściowy upload nie może zostać pokazany jako pełny sukces.

## Storage Rules

Rules powinny sprawdzać:

- zalogowanego użytkownika,
- zgodność UID ze ścieżką,
- dozwolony MIME,
- maksymalny rozmiar,
- dozwoloną operację,
- rolę administratora dla moderacji,
- brak zapisu do cudzej ścieżki.

Publiczny odczyt zależy od typu zasobu i statusu biznesowego. Avatar lub zdjęcie usuniętego albo zablokowanego konta nie może ujawniać danych prywatnych.

## Spójność z Firestore

- dokument biznesowy przechowuje wyłącznie odnośnik do istniejącego pliku,
- usunięcie pliku aktualizuje dokument i liczniki,
- plik bez poprawnego dokumentu jest traktowany jako osierocony,
- retry nie tworzy wielu kopii tego samego zdjęcia,
- cleanup jest idempotentny.

## Retencja i account deletion

- avatar i pliki prywatne są usuwane przy account deletion,
- publiczne zdjęcia są usuwane albo zachowywane zgodnie z polityką anonimizacji,
- usunięcie miejsca lub opinii obsługuje powiązane pliki,
- nieudany cleanup ma retry i monitoring,
- osierocone pliki są okresowo wykrywane,
- backup i retencja nie mogą bezterminowo omijać usunięcia danych.

## Monitoring

Monitorujemy:

- liczbę i rozmiar uploadów,
- błędy i timeouty,
- niewłaściwe MIME,
- błędy permission-denied,
- osierocone pliki,
- koszty storage i transferu,
- błędy cleanup po delete account.

Logi nie zawierają pełnych URI, nazw plików użytkownika ani danych osobowych.

## Testy

- poprawny upload właściciela,
- próba zapisu do cudzej ścieżki,
- zły MIME,
- przekroczony rozmiar,
- usunięcie własnego pliku,
- operacja administratora,
- częściowy błąd uploadu,
- cleanup miejsca, opinii i konta,
- brak szerokiego uprawnienia galerii.

## Checklista

- [ ] struktura ścieżek wspiera ownership,
- [ ] Rules chronią wszystkie ścieżki,
- [ ] MIME i rozmiar są walidowane,
- [ ] EXIF/GPS są usuwane, gdy niepotrzebne,
- [ ] retry nie tworzy duplikatów,
- [ ] cleanup jest zgodny z account deletion,
- [ ] osierocone pliki są monitorowane,
- [ ] logi nie zawierają PII,
- [ ] testy Storage Rules przechodzą.
