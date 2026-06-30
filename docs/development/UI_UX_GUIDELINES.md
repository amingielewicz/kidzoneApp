# UI / UX Guidelines

## Cel

Dokument definiuje standardy UI i UX aplikacji KidZone. Celem jest spójny, lekki i rodzinny interfejs, który prowadzi użytkownika bez zgadywania.

## Zasady UI

- Interfejs ma być jasny, lekki i czytelny.
- Karty powinny mieć spójny radius, padding i cień.
- Kategorie pokazujemy jako badge lub ikonę, nie jako ciężkie belki.
- Typografia musi mieć stałą hierarchię.
- Akcje główne muszą być widoczne i jednoznaczne.
- FAB powinien jasno komunikować akcję: `Dodaj miejsce`.

## Typografia

- Tytuł ekranu: wyraźny i krótki.
- Tytuł karty: semibold.
- Opis: krótki, bez ściany tekstu.
- Metadane: mniejsze, ale nadal czytelne.
- Komunikaty błędów: proste i bez technicznego żargonu.

## Karty miejsc

Karta miejsca powinna zawierać:

- nazwę miejsca,
- kategorię,
- ocenę,
- adres albo dystans,
- krótki opis,
- informację o liczbie opinii,
- jedno główne CTA, jeśli jest potrzebne.

## UX states

Każdy główny ekran musi mieć:

- loading state,
- empty state,
- error state,
- offline state, jeśli ekran zależy od sieci,
- permission state, jeśli ekran zależy od uprawnień.

## Empty states

Dobry empty state zawiera:

- prosty nagłówek,
- krótkie wyjaśnienie,
- sensowną akcję.

Przykład:

```text
Brak miejsc w okolicy
Nie znaleźliśmy jeszcze miejsc blisko Ciebie.
Dodaj pierwsze miejsce
```

## Onboarding

Onboarding powinien:

- wyjaśniać cel aplikacji,
- pokazać mapę, listę, ranking i profil,
- wyjaśnić dodawanie miejsca,
- nie blokować użytkownika,
- dać się pominąć.

## Dostępność UX

- Ikony akcji muszą mieć opis.
- Touch targety muszą być wygodne.
- Tekst musi działać przy powiększonej czcionce.
- Mapa musi mieć alternatywę w postaci listy.
- Kolory nie mogą być jedynym nośnikiem informacji.

## Checklist

- [ ] Główna akcja ekranu jest oczywista.
- [ ] Użytkownik wie, gdzie jest.
- [ ] Użytkownik wie, co kliknąć dalej.
- [ ] Ekran ma loading/empty/error state.
- [ ] Teksty są krótkie i zrozumiałe.
- [ ] UI jest spójny z resztą aplikacji.
