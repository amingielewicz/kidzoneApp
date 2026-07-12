import { useCallback, useEffect, useRef } from 'react';
import type { MouseEvent } from 'react';
import { Box } from '@mui/material';
import { ChangeRequestsPage } from './ChangeRequestsPage';
import { db } from '../services/firebase';

const FIREBASE_ICON_SVG = `
  <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true" focusable="false">
    <path fill="#FFCA28" d="M5.8 17.8 8.2 2.5c.1-.5.8-.6 1-.1l2.4 4.5-5.8 10.9Z"/>
    <path fill="#FFA000" d="m13.4 10.4 2.2-4.2c.2-.4.8-.4 1 .1l2 11.4-5.2-7.3Z"/>
    <path fill="#F57C00" d="m5.8 17.8 5.8-10.9 1.8 3.5 5.2 7.3-6.4 3.6-6.4-3.5Z"/>
  </svg>
`;

export function ChangeRequestsPageWithRowNavigation() {
  const rootRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const root = rootRef.current;
    if (!root) return;

    let enhancing = false;

    const enhanceTable = () => {
      if (enhancing) return;
      enhancing = true;

      try {
        const table = root.querySelector<HTMLTableElement>('table');
        const headerRow = table?.querySelector<HTMLTableRowElement>('thead tr');
        if (!table || !headerRow) return;

        const placeIdHeader = headerRow.children.item(2) as HTMLTableCellElement | null;
        if (placeIdHeader) {
          placeIdHeader.style.width = '1%';
          placeIdHeader.style.whiteSpace = 'nowrap';
        }

        if (!headerRow.querySelector('[data-firebase-column="true"]')) {
          const headerCell = document.createElement('th');
          headerCell.setAttribute('data-firebase-column', 'true');
          headerCell.setAttribute('scope', 'col');
          headerCell.className = 'MuiTableCell-root MuiTableCell-head MuiTableCell-sizeSmall';
          headerCell.textContent = 'Firebase';
          headerCell.style.width = '72px';
          headerCell.style.textAlign = 'center';
          headerCell.style.whiteSpace = 'nowrap';
          headerRow.appendChild(headerCell);
        }

        root.querySelectorAll<HTMLTableRowElement>('tbody .MuiTableRow-root').forEach((row) => {
          const detailsButton = row.querySelector<HTMLButtonElement>(
            'button[aria-label^="Pokaż szczegóły propozycji zmian "]',
          );

          if (!detailsButton) {
            const emptyCell = row.querySelector<HTMLTableCellElement>('td[colspan]');
            if (emptyCell && emptyCell.colSpan !== 7) emptyCell.colSpan = 7;
            return;
          }

          const requestId = detailsButton
            .getAttribute('aria-label')
            ?.replace('Pokaż szczegóły propozycji zmian ', '');
          if (!requestId) return;

          const copyButton = row.querySelector<HTMLButtonElement>(
            'button[aria-label^="Kopiuj ID miejsca "]',
          );
          const placeId = copyButton
            ?.getAttribute('aria-label')
            ?.replace('Kopiuj ID miejsca ', '');

          if (placeId && copyButton) {
            const placeIdCell = copyButton.closest<HTMLTableCellElement>('td');
            const placeIdText = placeIdCell?.querySelector<HTMLElement>('.MuiTypography-root');
            const placeIdContainer = copyButton.parentElement;

            if (placeIdText) {
              if (placeIdText.textContent !== placeId) {
                placeIdText.textContent = placeId;
              }
              placeIdText.style.whiteSpace = 'nowrap';
            }
            if (placeIdContainer) {
              placeIdContainer.style.width = 'max-content';
              placeIdContainer.style.flexWrap = 'nowrap';
            }
            if (placeIdCell) {
              placeIdCell.style.width = '1%';
              placeIdCell.style.minWidth = 'max-content';
              placeIdCell.style.whiteSpace = 'nowrap';
            }
          }

          if (row.querySelector('[data-firebase-cell="true"]')) return;

          const projectId = db.app.options.projectId || 'playground-705e7162';
          const firebaseUrl =
            `https://console.firebase.google.com/project/${projectId}` +
            `/firestore/data/place_change_requests/${requestId}`;

          const firebaseCell = document.createElement('td');
          firebaseCell.setAttribute('data-firebase-cell', 'true');
          firebaseCell.className = 'MuiTableCell-root MuiTableCell-body MuiTableCell-sizeSmall';
          firebaseCell.style.width = '72px';
          firebaseCell.style.textAlign = 'center';

          const link = document.createElement('a');
          link.href = firebaseUrl;
          link.target = '_blank';
          link.rel = 'noopener noreferrer';
          link.title = 'Otwórz w Firebase Console';
          link.setAttribute('aria-label', `Otwórz propozycję ${requestId} w Firebase Console`);
          link.style.display = 'inline-flex';
          link.style.alignItems = 'center';
          link.style.justifyContent = 'center';
          link.style.width = '36px';
          link.style.height = '36px';
          link.style.borderRadius = '50%';
          link.style.cursor = 'pointer';
          link.style.textDecoration = 'none';
          link.innerHTML = FIREBASE_ICON_SVG;

          firebaseCell.appendChild(link);
          row.appendChild(firebaseCell);
        });
      } finally {
        enhancing = false;
      }
    };

    enhanceTable();
    const observer = new MutationObserver(enhanceTable);
    observer.observe(root, { childList: true, subtree: true });
    return () => observer.disconnect();
  }, []);

  const handleClick = useCallback((event: MouseEvent<HTMLDivElement>) => {
    const target = event.target instanceof Element ? event.target : null;
    const row = target?.closest('tbody .MuiTableRow-root');
    if (!row || target?.closest('button, a, input, textarea, select, [role="button"]')) return;

    const detailsButton = row.querySelector<HTMLButtonElement>(
      'button[aria-label^="Pokaż szczegóły propozycji zmian "]',
    );
    detailsButton?.click();
  }, []);

  return (
    <Box
      ref={rootRef}
      onClick={handleClick}
      sx={{
        '& tbody .MuiTableRow-root:has(button[aria-label^="Pokaż szczegóły propozycji zmian "])': {
          cursor: 'pointer',
        },
        '& [data-firebase-cell="true"] a:hover': {
          backgroundColor: 'action.hover',
        },
      }}
    >
      <ChangeRequestsPage />
    </Box>
  );
}
