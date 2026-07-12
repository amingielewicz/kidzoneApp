import { KeyboardEvent, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box } from '@mui/material';
import { DashboardPage } from './DashboardPageV2';

const CARD_ROUTES: Record<string, string> = {
  Miejsca: '/places',
  Użytkownicy: '/users',
  'Propozycje zmian': '/change-requests',
  'Zgłoszenia oczekujące': '/reports',
};

export function DashboardPageWithNavigation() {
  const navigate = useNavigate();

  const getRoute = useCallback((target: EventTarget | null): string | null => {
    const card = target instanceof Element ? target.closest('.MuiCard-root') : null;
    if (!card) return null;

    const label = Object.keys(CARD_ROUTES).find((name) => card.textContent?.includes(name));
    if (!label) return null;

    return CARD_ROUTES[label] ?? null;
  }, []);

  const handleClick = useCallback(
    (event: React.MouseEvent<HTMLDivElement>) => {
      const route = getRoute(event.target);
      if (route) navigate(route);
    },
    [getRoute, navigate],
  );

  const handleKeyDown = useCallback(
    (event: KeyboardEvent<HTMLDivElement>) => {
      if (event.key !== 'Enter' && event.key !== ' ') return;
      const route = getRoute(event.target);
      if (!route) return;

      event.preventDefault();
      navigate(route);
    },
    [getRoute, navigate],
  );

  return (
    <Box
      onClick={handleClick}
      onKeyDown={handleKeyDown}
      sx={{
        '& > .MuiGrid-root:first-of-type > .MuiGrid-item:nth-of-type(1) .MuiCard-root, & > .MuiGrid-root:first-of-type > .MuiGrid-item:nth-of-type(3) .MuiCard-root, & > .MuiGrid-root:first-of-type > .MuiGrid-item:nth-of-type(4) .MuiCard-root, & > .MuiGrid-root:first-of-type > .MuiGrid-item:nth-of-type(5) .MuiCard-root': {
          cursor: 'pointer',
          transition: 'transform 0.15s ease, box-shadow 0.15s ease',
          '&:hover': {
            transform: 'translateY(-2px)',
            boxShadow: 4,
          },
        },
      }}
    >
      <DashboardPage />
    </Box>
  );
}
