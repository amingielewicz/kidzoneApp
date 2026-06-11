import { adminFetch } from './api';

/**
 * Cloud Functions base URL — constructed from environment config.
 *
 * Reads:
 *  - VITE_CF_REGION (default: "us-central1")
 *  - VITE_FIREBASE_PROJECT_ID (default: "playground-705e7162")
 *
 * Usage:
 *   await callFunction('adminDeletePlace', { placeId, reason });
 */
const CF_REGION = import.meta.env.VITE_CF_REGION || 'us-central1';
const PROJECT_ID = import.meta.env.VITE_FIREBASE_PROJECT_ID || 'playground-705e7162';
const CF_BASE_URL = `https://${CF_REGION}-${PROJECT_ID}.cloudfunctions.net`;

/**
 * Call a Cloud Function by name with query parameters.
 *
 * Automatically adds admin auth token via adminFetch.
 * Throws on non-2xx response.
 *
 * @param functionName - Cloud Function name (e.g. "adminDeletePlace")
 * @param params - Query parameters as key-value pairs
 * @returns Response object
 */
export async function callFunction(
  functionName: string,
  params: Record<string, string> = {},
): Promise<Response> {
  const searchParams = new URLSearchParams(params).toString();
  const url = searchParams
    ? `${CF_BASE_URL}/${functionName}?${searchParams}`
    : `${CF_BASE_URL}/${functionName}`;
  return adminFetch(url);
}
