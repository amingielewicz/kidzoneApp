(() => {
  const statElements = {
    users: document.querySelector('[data-stat="users"]'),
    places: document.querySelector('[data-stat="places"]'),
    reviews: document.querySelector('[data-stat="reviews"]'),
    supporters: document.querySelector('[data-stat="supporters"]'),
  };

  const note = document.querySelector('[data-stats-note]');
  const formatNumber = new Intl.NumberFormat('pl-PL');

  fetch('/api/public-stats', {headers: {'Accept': 'application/json'}})
    .then((response) => {
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      return response.json();
    })
    .then((stats) => {
      for (const [key, element] of Object.entries(statElements)) {
        if (!element) continue;
        const value = stats[key];
        element.textContent = Number.isFinite(value) ? formatNumber.format(value) : '—';
      }

      if (note) {
        note.textContent = stats.supporters == null
          ? 'Dane o użytkownikach, miejscach i opiniach są pobierane automatycznie. Licznik osób wspierających pojawi się po podłączeniu Suppi.'
          : 'Statystyki są aktualizowane automatycznie.';
      }
    })
    .catch((error) => {
      console.error('Nie udało się pobrać statystyk kidZone:', error);
      if (note) note.textContent = 'Nie udało się teraz pobrać statystyk. Spróbuj ponownie później.';
    });
})();
