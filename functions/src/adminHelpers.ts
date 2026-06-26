export type HttpRequestHeaders = {
  authorization?: string | string[];
};

export function extractBearerToken(headers: HttpRequestHeaders): string | null {
  const rawAuthHeader = headers.authorization || "";
  const authHeader = Array.isArray(rawAuthHeader) ? rawAuthHeader[0] || "" : rawAuthHeader;
  if (!authHeader.startsWith("Bearer ")) {
    return null;
  }
  const token = authHeader.slice("Bearer ".length).trim();
  return token || null;
}

export function escapeHtml(text: string): string {
  return text
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

export function mapReason(reason: string): string {
  const reasons: Record<string, string> = {
    "NOT_EXISTS": "Miejsce nie istnieje / zamknięte",
    "INAPPROPRIATE": "Nieodpowiednia treść (wulgaryzmy, reklama)",
    "DUPLICATE": "Duplikat innego miejsca",
    "FALSE_DATA": "Fałszywe dane (adres, udogodnienia)",
    "OTHER": "Inne",
  };
  const label = reasons[reason] || reason;
  return `${label} [${reason}]`;
}

export function mapReviewReason(reason: string): string {
  const reasons: Record<string, string> = {
    "SPAM": "Spam / reklama",
    "OFFENSIVE": "Obraźliwa treść",
    "FALSE_INFO": "Fałszywe informacje",
    "NOT_RELEVANT": "Nie dotyczy tego miejsca",
    "OTHER": "Inne",
  };
  const label = reasons[reason] || reason;
  return `${label} [${reason}]`;
}

export function mapPhotoReason(reason: string): string {
  const reasons: Record<string, string> = {
    "INAPPROPRIATE": "Nieodpowiednia treść",
    "NOT_RELEVANT": "Niezwiązane z miejscem",
    "COPYRIGHT": "Narusza prawa autorskie",
    "OFFENSIVE": "Obraźliwe / wulgarne",
    "OTHER": "Inne",
  };
  const label = reasons[reason] || reason;
  return `${label} [${reason}]`;
}

export function renderAdminResponse(title: string, message: string): string {
  return `<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>kidZone Admin - ${escapeHtml(title)}</title>
  <style>
    body { margin: 0; padding: 40px 20px; background: #f5f8fb; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; text-align: center; }
    .card { max-width: 500px; margin: 0 auto; background: #fff; border-radius: 12px; padding: 32px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
    h1 { color: #1976D2; margin-bottom: 12px; font-size: 24px; }
    p { color: #555; font-size: 16px; line-height: 1.5; }
    .logo { color: #1976D2; font-size: 14px; margin-top: 24px; font-weight: 600; }
  </style>
</head>
<body>
  <div class="card">
    <h1>${escapeHtml(title)}</h1>
    <p>${escapeHtml(message)}</p>
    <p class="logo">kidZone Admin Panel</p>
  </div>
</body>
</html>`;
}
