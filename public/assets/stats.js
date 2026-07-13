(() => {
  const values = {
    users: document.querySelector('[data-stat="users"]'),
    places: document.querySelector('[data-stat="places"]'),
    reviews: document.querySelector('[data-stat="reviews"]'),
    supporters: document.querySelector('[data-stat="supporters"]'),
  };
  const note = document.querySelector('[data-stats-note]');
  const numberFormat = new Intl.NumberFormat('pl-PL');

  const setValue = (key, value) => {
    const element = values[key];
    if (!element) return;
    element.textContent = Number.isFinite(value) ? numberFormat.format(value) : '—';
  };

  fetch('/api/public-stats', {
    method: 'GET',
    headers: { Accept: 'application/json' },
    cache: 'no-store',
  })
    .then((response) => {
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      return response.json();
    })
    .then((data) => {
      setValue('users', data.users);
      setValue('places', data.places);
      setValue('reviews', data.reviews);
      setValue('supporters', data.supporters);
      if (note) note.textContent = 'Aktualne dane społeczności kidZone.';
    })
    .catch((error) => {
      console.error('Nie udało się pobrać statystyk kidZone:', error);
      setValue('users', null);
      setValue('places', null);
      setValue('reviews', null);
      setValue('supporters', null);
      if (note) note.textContent = 'Nie udało się pobrać statystyk. Spróbuj ponownie później.';
    });
})();
