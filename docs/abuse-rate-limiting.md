# Abuse and rate limiting strategy

Powiazane issue: #281

## Cel

Ten dokument opisuje strategię MVP dla ograniczania spamu, nadużyć i kosztów
przy treściach tworzonych przez użytkowników w kidZone.

Firestore Rules i Storage Rules chronią integralność danych, ale nie są pełnym
systemem rate limitingu. Limity czasowe, liczniki dzienne i wykrywanie anomalii
powinny być egzekwowane przez backend, Cloud Functions albo dedykowane procesy
operacyjne.

## Obszary ryzyka

- `places`: spam miejscami i fałszywe lokalizacje.
- `reviews`: spam opiniami i manipulacja ocenami.
- `place_reports`, `review_reports`, `photo_reports`: zalew panelu admina.
- `place_change_requests`: spam zmianami danych miejsca.
- Storage uploads: koszty transferu i przechowywania.
- `users/{uid}/private/messaging`: nadmiarowe zapisy tokenów FCM.

## Limity MVP

Rekomendowane limity startowe per użytkownik:

| Obszar | Limit MVP | Egzekucja |
| --- | ---: | --- |
| Nowe miejsca | 10 dziennie | Cloud Function / backend |
| Opinie | 30 dziennie | Cloud Function / backend |
| Zgłoszenia | 50 dziennie | Cloud Function / backend |
| Zdjęcia miejsc i opinii | 40 dziennie | Cloud Function / backend + Storage Rules |
| Avatar | 10 zmian dziennie | Cloud Function / backend + Storage Rules |
| Rozmiar zdjęcia miejsca/opinii | < 10 MB | Storage Rules |
| Rozmiar avatara | < 5 MB | Storage Rules |

Limity powinny być traktowane jako wartości początkowe. Po testach beta należy
porównać je z realnym użyciem i dostosować, żeby nie blokować aktywnych,
uczciwych użytkowników.

## Co egzekwują reguły Firebase

Firestore Rules:

- właściciel może tworzyć dane tylko jako własny `uid`,
- zwykły użytkownik nie może ustawić sobie roli admina,
- publiczny profil nie może zawierać prywatnych pól,
- użytkownik nie może zmieniać cudzych miejsc,
- użytkownik nie może podmienić zdjęć cudzego miejsca,
- ocena opinii musi być w zakresie `1..5` przy create i update,
- zgłoszenie musi mieć `reporterId == request.auth.uid`.

Storage Rules:

- upload jest dozwolony tylko do własnej ścieżki,
- dopuszczalne są tylko `image/jpeg`, `image/png`, `image/webp`,
- zdjęcia miejsc i opinii mają limit < 10 MB,
- avatary mają limit < 5 MB,
- legacy paths są read-only dla zwykłych użytkowników,
- owner albo admin może usuwać pliki w ścieżkach właścicielskich.

## Czego reguły nie zrobią dobrze

Reguły Firebase nie powinny być jedynym mechanizmem dla:

- limitów dziennych i godzinowych,
- liczników per użytkownik,
- wykrywania automatyzacji,
- banów progresywnych,
- alertów kosztowych,
- analizy reputacji konta,
- moderacji treści.

Te elementy powinny zostać zrobione osobno, najlepiej w Cloud Functions albo
warstwie backendowej.

## App Check

App Check powinien zostać włączony etapami:

1. Dodać debug tokeny dla urządzeń deweloperskich.
2. Zweryfikować debug build na Firestore, Storage i Cloud Functions.
3. Włączyć enforcement najpierw dla Storage albo Firestore na środowisku testowym.
4. Sprawdzić logowanie, mapę, listę, dodawanie miejsca, opinii i upload zdjęć.
5. Dopiero potem rozszerzyć enforcement na produkcję.

Szczegóły są w `docs/app-check.md`.

## Monitoring i reakcja

Minimalny monitoring MVP:

- alert kosztowy Google Cloud Billing,
- regularny przegląd wzrostu kolekcji zgłoszeń,
- regularny przegląd rozmiaru Storage,
- logowanie odrzuconych operacji po stronie aplikacji bez danych wrażliwych,
- ręczna możliwość zablokowania użytkownika (`role`, `bannedUntilMillis`).

Reakcja admina na spam:

1. Zidentyfikować konto i typ nadużycia.
2. Zablokować konto, jeśli nadużycie jest oczywiste.
3. Usunąć lub ukryć treści.
4. Sprawdzić powiązane zdjęcia w Storage.
5. Dodać test regresji albo limit, jeśli nadużycie wykorzystało lukę.

## Testy regresji

Testy emulatora dla reguł znajdują się w `tests/firestore-rules`.

Komenda:

```powershell
firebase emulators:exec --only firestore,storage "npm --prefix tests/firestore-rules test"
```

Alternatywnie, gdy emulatory już działają:

```powershell
cd tests/firestore-rules
npm test
```

## Otwarte decyzje po MVP

- Czy limity egzekwować w Cloud Functions przed zapisem, czy przez zadania
  kontrolne po zapisie.
- Czy wprowadzić reputację konta i ostrzejsze limity dla nowych użytkowników.
- Czy zdjęcia użytkowników powinny przechodzić przez kolejkę moderacji.
- Czy admin panel ma mieć widok anomalii i nadużyć.
