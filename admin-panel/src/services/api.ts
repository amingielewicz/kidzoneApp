import { auth } from './firebase';

/**
 * Fetch z automatycznym dodaniem tokena admina w nagłówku Authorization.
 */
export async function adminFetch(url: string): Promise<Response> {
  const user = auth.currentUser;
  if (!user) throw new Error('Nie zalogowany');
  const token = await user.getIdToken();
  return fetch(url, {
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  });
}
