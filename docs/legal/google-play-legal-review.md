# Google Play legal review przed publikacją

Powiązane issue: #182, #210, #269, #272, #274, #275, #303

Milestone: `v1.0.0` — publiczny release produkcyjny

Ostatnia aktualizacja: 2026-07-13

## Cel

Dokument zbiera techniczno-produktową checklistę zgodności kidZone przed publikacją w Google Play. Nie zastępuje porady prawnej.

Szczegółowa kolejność ręcznych bramek znajduje się w:

`docs/legal/v1-release-manual-gates.md`

## Status ogólny

| Obszar | Status | Uwagi |
| --- | --- | --- |
| Regulamin | gotowy w repo | wymaga finalnego sprawdzenia publicznego URL |
| Polityka prywatności | gotowa w repo | wymaga potwierdzenia zgodności z aktywnymi SDK |
| Usuwanie konta | gotowe w repo | nadal wymagany test manualny |
| Lokalizacja | gotowa technicznie | nadal wymagany test odmów i powrotu z ustawień |
| Kamera | gotowa technicznie | nadal wymagany test trwałej odmowy |
| Zdjęcia | gotowe technicznie | aplikacja używa Android Photo Pickera i nie deklaruje szerokiego dostępu do galerii |
| Google Play Data Safety | draft gotowy | trzeba przepisać i potwierdzić w Play Console |
| Finalny przegląd prawny | otwarty | do wykonania przed publikacją |

## Stan potwierdzony w repozytorium

| Obszar | Stan | Evidence |
| --- | --- | --- |
| Regulamin | dokument istnieje | `public/terms-of-service.html` |
| Polityka prywatności | dokument istnieje | `public/privacy-policy.html` |
| Usuwanie konta | publiczna strona istnieje | `public/account-deletion.html` |
| Data Safety | draft istnieje | `docs/legal/google-play-data-safety-draft.md` |
| Android permissions | audyt istnieje | `docs/legal/android-permissions-play-compliance.md` |
| Manual release gates | runbook istnieje | `docs/legal/v1-release-manual-gates.md` |
| Lokalizacja | tylko foreground | `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`; brak `ACCESS_BACKGROUND_LOCATION` |
| Zdjęcia | systemowy Photo Picker | `PickVisualMedia`, `PickMultipleVisualMedia` |
| Kamera | funkcja opcjonalna | `CAMERA`, `android.hardware.camera` z `required="false"` |
| Powiadomienia | Android 13+ runtime permission | `POST_NOTIFICATIONS` |

Elementy wymagające pracy poza repozytorium:

- test usuwania konta na urządzeniu i w Firebase Console,
- weryfikacja publicznych URL po deployu,
- test systemowych dialogów zgód,
- zapisanie finalnych odpowiedzi w Google Play Console,
- finalny przegląd prawny dokumentów.

## 1. Regulamin

Plik: `public/terms-of-service.html`

Zakres:

- operator aplikacji,
- pełnoletnia grupa docelowa,
- konto użytkownika,
- zasady publikowania treści,
- zdjęcia, wizerunek i bezpieczeństwo dzieci,
- blokowanie konta,
- zgłaszanie naruszeń,
- odznaki i rankingi,
- odpowiedzialność,
- kontakt.

Do sprawdzenia:

- finalne dane operatora,
- stabilny publiczny URL,
- zgodność z aktualnymi funkcjami aplikacji,
- opis blokowania konta i odwołań,
- widoczność zasad publikowania zdjęć dzieci również w UI.

## 2. Polityka prywatności

Plik: `public/privacy-policy.html`

Zakres:

- dane konta,
- dane lokalizacyjne,
- treści użytkownika,
- dane techniczne i diagnostyczne,
- cele i podstawy przetwarzania,
- usługi Firebase i Google,
- retencja,
- prawa użytkownika,
- dzieci,
- usunięcie konta i danych,
- kontakt.

Do sprawdzenia:

- zgodność listy usług z realnie aktywnymi SDK,
- opis Analytics, Crashlytics, Performance, App Check i FCM,
- realność deklarowanych okresów retencji,
- zgodność z Google Play Data Safety,
- publiczny dostęp bez logowania.

## 3. Usuwanie konta

Ścieżki:

- aplikacja: `Profil → Konto i bezpieczeństwo → Usuń konto`,
- strona publiczna: `/account-deletion`.

Do weryfikacji:

- ponowne uwierzytelnienie, jeśli wymaga go Firebase,
- usunięcie lub anonimizacja danych prywatnych,
- zachowanie publicznych treści użytkownika,
- sprzątanie zdjęć i danych prywatnych,
- brak danych użytkownika w lokalnym cache po restarcie.

Adresy do Google Play Console:

```text
Privacy Policy URL: https://playground-705e7162.web.app/privacy-policy
Account deletion URL: https://playground-705e7162.web.app/account-deletion
Terms URL: https://playground-705e7162.web.app/terms-of-service
```

## 4. Lokalizacja

Aplikacja używa lokalizacji do mapy, miejsc w pobliżu, sortowania po odległości oraz dodawania miejsca.

Stan repozytorium:

- deklarowane są `ACCESS_FINE_LOCATION` i `ACCESS_COARSE_LOCATION`,
- brak `ACCESS_BACKGROUND_LOCATION`,
- lokalizacja jest pobierana tylko w aktywnym flow użytkownika,
- wspólna funkcja `requestLocationPermissionOrOpenSettings` obsługuje ponowną próbę i trwałą odmowę,
- po trwałej odmowie aplikacja powinna otwierać ustawienia aplikacji.

Do weryfikacji:

- pierwsza i kolejna odmowa,
- trwała odmowa,
- powrót z ustawień po nadaniu zgody,
- approximate i precise,
- wyłączony GPS przy nadanym uprawnieniu,
- spójność na ekranie Start, Mapie, Liście oraz w formularzu miejsca,
- brak martwych przycisków po kolejnych odmowach.

Docelowy zakres:

```text
Lokalizacja tylko podczas używania aplikacji.
Brak lokalizacji w tle.
Brak historii lokalizacji użytkownika.
```

## 5. Zdjęcia i kamera

Aplikacja pozwala dodawać zdjęcia miejsc, opinii i avatarów.

Stan repozytorium:

- avatar używa `PickVisualMedia`,
- dodawanie miejsca i opinii używa `PickMultipleVisualMedia`,
- szczegóły miejsca używają `PickVisualMedia`,
- kamera używa `TakePicture` i osobnego runtime permission `CAMERA`,
- manifest nie deklaruje `READ_MEDIA_IMAGES`,
- manifest nie deklaruje `READ_EXTERNAL_STORAGE`,
- użytkownik wybiera konkretne zdjęcia przez systemowy Android Photo Picker.

Decyzja:

```text
READ_MEDIA_IMAGES nie jest potrzebne i nie powinno być deklarowane.
```

Do weryfikacji:

- Photo Picker nie wyświetla szerokiego dialogu dostępu do galerii,
- wybór zdjęć działa bez zgody na kamerę,
- trwała odmowa kamery prowadzi do ustawień aplikacji,
- zdjęcia można zgłaszać i usuwać zgodnie z uprawnieniami,
- Storage Rules blokują modyfikowanie cudzych plików,
- polityka prywatności i Data Safety obejmują zdjęcia użytkowników,
- UI przypomina o prawach do wizerunku dzieci i osób trzecich.

## 6. Google Play Data Safety

Dokument roboczy:

`docs/legal/google-play-data-safety-draft.md`

Draft powinien zostać porównany z:

- aktywnymi usługami Firebase,
- finalnym manifestem,
- polityką prywatności,
- realnym zachowaniem aplikacji,
- ustawieniami w Google Play Console.

Dane potencjalnie deklarowane:

| Kategoria | Czy dotyczy | Uwagi |
| --- | --- | --- |
| Email | tak | konto użytkownika |
| Nazwa użytkownika | tak | profil, treści, ranking |
| Imię i nazwisko | opcjonalnie | zależnie od profilu |
| Zdjęcia | tak | miejsca, opinie, avatar |
| Lokalizacja | tak | mapa i miejsca w pobliżu |
| Treści użytkownika | tak | miejsca, opinie, zgłoszenia |
| Crash logs i diagnostyka | tak, jeśli aktywne | Crashlytics i Performance |
| Aktywność w aplikacji | tak, jeśli aktywne | Analytics |
| Identyfikatory urządzenia | możliwe | Firebase, FCM, Google Play Services |

Do potwierdzenia:

- dane zbierane i udostępniane,
- opcjonalność danych,
- szyfrowanie w transmisji,
- możliwość usunięcia danych,
- cel użycia danych,
- zgodność odpowiedzi z polityką prywatności.

## 7. Braki i ryzyka

| Ryzyko | Poziom | Działanie |
| --- | --- | --- |
| Data Safety nieuzupełnione w Play Console | wysokie | przepisać i potwierdzić draft |
| Nieprzetestowane usuwanie konta | wysokie | wykonać checklistę #210 |
| Nieprzetestowane runtime permissions | wysokie | wykonać #274 i #303 |
| Zdjęcia dzieci i osób trzecich | wysokie | zapewnić widoczne ostrzeżenie w UI |
| Retencja danych | średnie | potwierdzić techniczną wykonalność |
| Publiczne URL po deployu | średnie | sprawdzić w incognito |
| Finalny przegląd prawny | wysokie | wykonać przed produkcją |

`READ_MEDIA_IMAGES` nie jest już otwartym ryzykiem ani decyzją. Uprawnienie zostało usunięte, a aplikacja korzysta z Android Photo Pickera.

## 8. Checklista końcowa #182

- [x] Regulamin istnieje i obejmuje główne obszary.
- [x] Polityka prywatności istnieje i obejmuje główne obszary.
- [x] Draft Google Play Data Safety istnieje.
- [x] Decyzja o `READ_MEDIA_IMAGES` została wykonana: uprawnienie usunięte.
- [ ] Usuwanie konta przetestowane na urządzeniu i w Firebase.
- [ ] Lokalizacja przetestowana dla allow, deny i trwałej odmowy.
- [ ] Kamera przetestowana dla allow, deny i trwałej odmowy.
- [ ] Photo Picker przetestowany na Androidzie 13+.
- [ ] Data Safety zapisane w Google Play Console.
- [ ] Finalny przegląd prawny wykonany.

## 9. Wniosek

Dokumenty publiczne oraz implementacja uprawnień stanowią dobrą bazę do wydania. Główne otwarte blokery to:

1. test usuwania konta,
2. manualny test runtime permissions,
3. zapisanie Data Safety w Play Console,
4. finalny przegląd prawny,
5. potwierdzenie publicznych URL po deployu.

Kwestia `READ_MEDIA_IMAGES` jest zamknięta. Aplikacja nie deklaruje szerokiego dostępu do galerii i korzysta z systemowego Android Photo Pickera.
