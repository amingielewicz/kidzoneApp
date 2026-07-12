import { MouseEvent, useCallback } from 'react';
import { Box } from '@mui/material';
import { ChangeRequestsPage } from './ChangeRequestsPage';

export function ChangeRequestsPageWithRowNavigation() {
  const handleClick = useCallback((event: MouseEvent<HTMLDivElement>) => {
    const target = event.target instanceof Element ? event.target : null;
    const row = target?.closest('tbody .MuiTableRow-root');
    if (!row || target?.closest('button, a, input, textarea, select, [role="button"]')) return;

    const detailsButton = row.querySelector<HTMLButtonElement>(
      'button[aria-label^="Pokaż szczegóły propozycji zmian"]',
    );
    detailsButton?.click();
  }, []);

  return (
    <Box
      onClick={handleClick}
      sx={{
        '& tbody .MuiTableRow-root': {
          cursor: 'pointer',
        },
      }}
    >
      <ChangeRequestsPage />
    </Box>
  );
}
