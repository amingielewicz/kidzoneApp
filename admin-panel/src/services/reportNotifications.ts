/**
 * 🎯 Cel: Zarządzanie stanem wyczyszczenia powiadomień o raportach w przeglądarce.
 * ⚡ Zarządzanie stanem: Wykorzystuje localStorage do trwałego zapisu czasu czyszczenia.
 * 📤 Wyjście: Zdarzenie niestandardowe 'REPORTS_CLEARED_EVENT'.
 */
export const REPORTS_CLEARED_EVENT = 'kidzone:dashboard-reports-cleared';
const REPORTS_CLEARED_AT_KEY = 'kidzone:dashboard-reports-cleared-at';

export function getDashboardReportsClearedAt(): number {
  const value = Number(localStorage.getItem(REPORTS_CLEARED_AT_KEY) || 0);
  return Number.isFinite(value) ? value : 0;
}

export function clearDashboardReports(): void {
  localStorage.setItem(REPORTS_CLEARED_AT_KEY, String(Date.now()));
  window.dispatchEvent(new Event(REPORTS_CLEARED_EVENT));
}
