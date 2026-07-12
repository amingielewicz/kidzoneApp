import { KeyboardEvent, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box } from '@mui/material';
import { DashboardPage } from './DashboardPageV2';

const CARD_ROUTES: Record<string, string> = {
  Miejsca: '/places',
  Użytkownicy: '/users',
  'Zgłoszenia oczekujące': '/reports',
};

export function DashboardPageWithNavigation() {
  const navigate = useNavigate();

  const getRoute = useCallback((target: EventTarget | null): string | null => {
    const card = target instanceof Element ? target.closest('.MuiCard-root') : null;
    if (!card) return null;

    const label = Object.keys(CARD_ROUTES).find((name) => card.textContent?.includes(name));
    return label ? CARD_ROUTES[label] : null;
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
        '& .MuiCard-root:has(.MuiTypography-root:nth-of-type(2))': {
          transition: 'box-shadow 0.2s ease, transform 0.2s ease',
        },
        '& .MuiCard-root': {
          '&:has(.MuiTypography-root:last-child)': {
            cursor: 'default',
          },
        },
      }}
    >
      <DashboardPage />
    </Box>
  );
}
