import { auth } from './firebase';

/**
 * Fetch z automatycznym dodaniem tokena admina w nagłówku Authorization.
 *
 * Rzuca Error jeśli:
 *  - user nie jest zalogowany,
 *  - Cloud Function zwróciła status 4xx/5xx.
 *
 * Caller powinien obsłużyć te błędy (try/catch lub .catch()).
 */
export async function adminFetch(url: string, options?: RequestInit): Promise<Response> {
  const user = auth.currentUser;
  if (!user) throw new Error('Nie zalogowany');
  const token = await user.getIdToken();

  const response = await fetch(url, {
    ...options,
    headers: {
      'Authorization': `Bearer ${token}`,
      ...options?.headers,
    },
  });

  if (!response.ok) {
    const body = await response.text().catch(() => '');
    throw new Error(
      `Admin API error: ${response.status} ${response.statusText}${body ? ` — ${body}` : ''}`
    );
  }

  return response;
}
