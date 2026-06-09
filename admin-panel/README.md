# kidZone Admin Panel

Panel administracyjny do zarządzania aplikacją kidZone.

## Funkcje

- **Dashboard** — statystyki: liczba miejsc, opinii, użytkowników, oczekujących zgłoszeń
- **Zgłoszenia** — zarządzanie zgłoszeniami (miejsca, opinie, zdjęcia): rozwiązanie lub odrzucenie
- **Miejsca** — lista wszystkich miejsc z wyszukiwarką, podgląd szczegółów, usuwanie
- **Użytkownicy** — lista użytkowników z wyszukiwarką

## Wymagania

- Node.js 20+
- Konto Firebase z rolą `admin` (pole `role: "admin"` w dokumencie użytkownika w kolekcji `users`)

## Instalacja

```bash
cd admin-panel
npm install
```

## Konfiguracja

Skopiuj `.env.example` do `.env` i uzupełnij wartości:

```bash
cp .env.example .env
```

Klucze Firebase znajdziesz w Firebase Console → Project Settings → General → Your apps → Web app.

## Uruchomienie

```bash
npm run dev
```

Panel będzie dostępny pod `http://localhost:5173`

## Deploy (Firebase Hosting)

```bash
npm run build
```

Pliki produkcyjne trafią do `dist/`. Możesz je skopiować do `public/admin/` i deployować z Firebase Hosting, lub ustawić osobny target w `firebase.json`.

## Nadanie roli admin

W Firebase Console → Firestore → Collection `users` → dokument użytkownika:
dodaj pole `role` o wartości `"admin"`.

## Firestore Security Rules

Panel wymaga zaktualizowanych reguł Firestore (dodane w tym PR) — admin może czytać/edytować zgłoszenia oraz usuwać miejsca i opinie.
