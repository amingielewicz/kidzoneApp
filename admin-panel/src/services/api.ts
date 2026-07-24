import { auth } from './firebase';

/**
 * 🎯 Cel: Fetch z automatycznym dodaniem tokena admina w nagłówku Authorization.
 *
 * 📥 Parametry:
 * - url: Adres endpointu (zwykle Cloud Function).
 * - options: Standardowe opcje fetch.
 *
 * 📤 Zwraca: Promise<Response>.
 *
 * 🛡️ Autoryzacja:
 * - Pobiera aktualny ID Token z Firebase Auth.
 * - Dołącza nagłówek 'Authorization: Bearer <token>'.
 * - Rzuca błąd, gdy użytkownik nie jest zalogowany lub status >= 400.
 */
export async function adminFetch(url: string, options?: RequestInit): Promise<Response> {
  const user = auth.currentUser;
  if (!user) throw new Error('Nie zalogowany');
  const token = await user.getIdToken();

  const response = await fetch(url, {
    ...options,
    headers: {
      Authorization: `Bearer ${token}`,
      ...options?.headers,
    },
  });

  if (!response.ok) {
    const body = await response.text().catch(() => '');
    throw new Error(
      `Admin API error: ${response.status} ${response.statusText}${body ? ` — ${body}` : ''}`,
    );
  }

  return response;
}
