import { useCallback, useEffect, useRef } from 'react';
import type { MouseEvent } from 'react';
import { Box } from '@mui/material';
import { doc, getDoc } from 'firebase/firestore';
import { ChangeRequestsPage } from './ChangeRequestsPage';
import { db } from '../services/firebase';

const DESCRIPTION_MAX_LENGTH = 72;

const FIREBASE_ICON_SVG = `
  <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true" focusable="false">
    <path fill="#FFCA28" d="M5.8 17.8 8.2 2.5c.1-.5.8-.6 1-.1l2.4 4.5-5.8 10.9Z"/>
    <path fill="#FFA000" d="m13.4 10.4 2.2-4.2c.2-.4.8-.4 1 .1l2 11.4-5.2-7.3Z"/>
    <path fill="#F57C00" d="m5.8 17.8 5.8-10.9 1.8 3.5 5.2 7.3-6.4 3.6-6.4-3.5Z"/>
  </svg>
`;

const AMENITY_LABELS: Record<string, string> = {
  CHANGING_TABLE: 'Przewijak',
  TOILET: 'Czysta toaleta',
  STROLLER_ACCESS: 'Dostęp dla wózka',
  PARKING: 'Parking',
  FENCING: 'Ogrodzenie',
  SOFT_SURFACE: 'Miękka nawierzchnia',
  SHADED_BENCHES: 'Ławki w cieniu',
  TODDLER_ZONE: 'Strefa 0–3',
  CAR_FREE_AREA: 'Brak ruchu samochodowego',
  KIDS_MENU: 'Menu dziecięce',
  HIGH_CHAIR: 'Krzesełka do karmienia',
  KIDS_TABLEWARE: 'Naczynia dziecięce',
  FAST_SERVICE: 'Szybka obsługa',
  KIDS_ENTERTAINMENT: 'Kredki, zabawki',
  KIDS_CORNER_VISIBLE: 'Kącik widoczny od stolika',
  AGE_ZONES: 'Podział na strefy wiekowe',
  ANIMATOR: 'Animator',
  MONITORING: 'Monitoring',
  TOY_SANITIZATION: 'Dezynfekcja zabawek',
  PARENT_ZONE: 'Strefa dla rodziców',
  LOCKERS: 'Szafki na rzeczy',
  WIFI: 'WiFi',
};

const CATEGORY_LABELS: Record<string, string> = {
  PLAYGROUND: 'Plac zabaw',
  PLAY_ROOM: 'Sala zabaw',
  CAFE: 'Kawiarnia rodzinna',
  RESTAURANT: 'Restauracja',
  PARK: 'Park',
  ATTRACTION: 'Atrakcja',
  OTHER: 'Inne',
};

function formatProposedValue(key: string, value: unknown): string {
  if (key === 'amenities' && Array.isArray(value)) {
    return value.map((item) => AMENITY_LABELS[String(item)] || String(item)).join(', ');
  }

  if (key === 'category') {
    return CATEGORY_LABELS[String(value)] || String(value);
  }

  if (Array.isArray(value)) {
    return value.map(String).join(', ');
  }

  return String(value ?? '').trim();
}

function getProposedChangesDescription(data: Record<string, unknown>): string {
  const changes = data.changes as Record<string, unknown> | undefined;
  if (!changes) return '—';

  const values = Object.entries(changes)
    .map(([key, value]) => formatProposedValue(key, value))
    .filter((value) => value.length > 0);

  return values.join(', ') || '—';
}

function setDescriptionContent(cell: HTMLTableCellElement, value: string): void {
  cell.replaceChildren();
  cell.title = value;

  const text = document.createElement('span');
  text.setAttribute('data-description-text', 'true');

  if (value.length <= DESCRIPTION_MAX_LENGTH) {
    text.textContent = value;
    cell.appendChild(text);
    return;
  }

  text.textContent = value.slice(0, DESCRIPTION_MAX_LENGTH).trimEnd();
  const ellipsis = document.createElement('span');
  ellipsis.setAttribute('data-description-ellipsis', 'true');
  ellipsis.textContent = '...';

  cell.append(text, ellipsis);
}

function formatDateCell(cell: HTMLTableCellElement): void {
  if (cell.hasAttribute('data-date-formatted')) return;

  const value = cell.textContent?.trim() || '';
  const match = value.match(/^(\d{2}\.\d{2}\.\d{4}),\s*(\d{2}:\d{2})$/);
  if (!match) return;

  const date = document.createElement('span');
  date.setAttribute('data-date-part', 'true');
  date.textContent = match[1];

  const time = document.createElement('span');
  time.setAttribute('data-time-part', 'true');
  time.textContent = match[2];

  cell.replaceChildren(date, time);
  cell.setAttribute('data-date-formatted', 'true');
}

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

        table.setAttribute('data-change-requests-table', 'true');

        (headerRow.children.item(0) as HTMLTableCellElement | null)?.setAttribute(
          'data-date-column',
          'true',
        );
        (headerRow.children.item(1) as HTMLTableCellElement | null)?.setAttribute(
          'data-type-column',
          'true',
        );
        (headerRow.children.item(2) as HTMLTableCellElement | null)?.setAttribute(
          'data-place-id-column',
          'true',
        );
        (headerRow.children.item(3) as HTMLTableCellElement | null)?.setAttribute(
          'data-changes-column',
          'true',
        );

        if (!headerRow.querySelector('[data-description-column="true"]')) {
          const descriptionHeader = document.createElement('th');
          descriptionHeader.setAttribute('data-description-column', 'true');
          descriptionHeader.setAttribute('scope', 'col');
          descriptionHeader.className = 'MuiTableCell-root MuiTableCell-head MuiTableCell-sizeSmall';
          descriptionHeader.textContent = 'Opis';
          headerRow.insertBefore(descriptionHeader, headerRow.children.item(4));
        }

        (headerRow.children.item(5) as HTMLTableCellElement | null)?.setAttribute(
          'data-status-column',
          'true',
        );
        (headerRow.children.item(6) as HTMLTableCellElement | null)?.setAttribute(
          'data-actions-column',
          'true',
        );

        if (!headerRow.querySelector('[data-firebase-column="true"]')) {
          const firebaseHeader = document.createElement('th');
          firebaseHeader.setAttribute('data-firebase-column', 'true');
          firebaseHeader.setAttribute('scope', 'col');
          firebaseHeader.className = 'MuiTableCell-root MuiTableCell-head MuiTableCell-sizeSmall';
          firebaseHeader.textContent = 'Firebase';
          headerRow.appendChild(firebaseHeader);
        }

        root.querySelectorAll<HTMLTableRowElement>('tbody .MuiTableRow-root').forEach((row) => {
          const detailsButton = row.querySelector<HTMLButtonElement>(
            'button[aria-label^="Pokaż szczegóły propozycji zmian "]',
          );

          if (!detailsButton) {
            const emptyCell = row.querySelector<HTMLTableCellElement>('td[colspan]');
            if (emptyCell && emptyCell.colSpan !== 8) emptyCell.colSpan = 8;
            return;
          }

          const requestId = detailsButton
            .getAttribute('aria-label')
            ?.replace('Pokaż szczegóły propozycji zmian ', '');
          if (!requestId) return;

          const dateCell = row.children.item(0) as HTMLTableCellElement | null;
          if (dateCell) {
            dateCell.setAttribute('data-date-cell', 'true');
            formatDateCell(dateCell);
          }

          (row.children.item(1) as HTMLTableCellElement | null)?.setAttribute(
            'data-type-cell',
            'true',
          );

          const copyButton = row.querySelector<HTMLButtonElement>(
            'button[aria-label^="Kopiuj ID miejsca "]',
          );
          const placeId = copyButton
            ?.getAttribute('aria-label')
            ?.replace('Kopiuj ID miejsca ', '');

          if (placeId && copyButton) {
            const placeIdCell = copyButton.closest<HTMLTableCellElement>('td');
            const placeIdText = placeIdCell?.querySelector<HTMLElement>('.MuiTypography-root');
            if (placeIdText && placeIdText.textContent !== placeId) {
              placeIdText.textContent = placeId;
            }
            placeIdCell?.setAttribute('data-place-id-cell', 'true');
          }

          const changesCell = row.children.item(3) as HTMLTableCellElement | null;
          changesCell?.setAttribute('data-changes-cell', 'true');

          let descriptionCell = row.querySelector<HTMLTableCellElement>('[data-description-cell="true"]');
          if (!descriptionCell) {
            descriptionCell = document.createElement('td');
            descriptionCell.setAttribute('data-description-cell', 'true');
            descriptionCell.className = 'MuiTableCell-root MuiTableCell-body MuiTableCell-sizeSmall';
            setDescriptionContent(descriptionCell, 'Ładowanie…');
            row.insertBefore(descriptionCell, row.children.item(4));

            void getDoc(doc(db, 'place_change_requests', requestId))
              .then((snapshot) => {
                const value = snapshot.exists()
                  ? getProposedChangesDescription(snapshot.data() as Record<string, unknown>)
                  : '—';
                setDescriptionContent(descriptionCell!, value);
              })
              .catch(() => {
                setDescriptionContent(descriptionCell!, '—');
              });
          }

          (row.children.item(5) as HTMLTableCellElement | null)?.setAttribute(
            'data-status-cell',
            'true',
          );
          (row.children.item(6) as HTMLTableCellElement | null)?.setAttribute(
            'data-actions-cell',
            'true',
          );

          if (row.querySelector('[data-firebase-cell="true"]')) return;

          const projectId = db.app.options.projectId || 'playground-705e7162';
          const firebaseUrl =
            `https://console.firebase.google.com/project/${projectId}` +
            `/firestore/data/place_change_requests/${requestId}`;

          const firebaseCell = document.createElement('td');
          firebaseCell.setAttribute('data-firebase-cell', 'true');
          firebaseCell.className = 'MuiTableCell-root MuiTableCell-body MuiTableCell-sizeSmall';

          const link = document.createElement('a');
          link.href = firebaseUrl;
          link.target = '_blank';
          link.rel = 'noopener noreferrer';
          link.title = 'Otwórz w Firebase Console';
          link.setAttribute('aria-label', `Otwórz propozycję ${requestId} w Firebase Console`);
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
        '& button[aria-label^="Pokaż szczegóły propozycji zmian "]': {
          display: 'none',
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
