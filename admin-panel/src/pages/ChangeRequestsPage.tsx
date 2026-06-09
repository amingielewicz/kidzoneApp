import { useEffect, useState } from 'react';
import {
  Box,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  IconButton,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Tooltip,
  ToggleButtonGroup,
  ToggleButton,
  TableSortLabel,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import VisibilityIcon from '@mui/icons-material/Visibility';
import {
  collection,
  query,
  orderBy,
  getDocs,
  doc,
  updateDoc,
  getDoc,
} from 'firebase/firestore';
import { db } from '../services/firebase';
import { PlaceChangeRequest, PLACE_CATEGORY_LABELS, PlaceCategory, ReportStatus } from '../types';

function statusChip(status: string) {
  switch (status) {
    case 'pending':
      return <Chip label="Oczekuje" color="warning" size="small" />;
    case 'resolved':
      return <Chip label="Zatwierdzone" color="success" size="small" />;
    case 'dismissed':
      return <Chip label="Odrzucone" color="default" size="small" />;
    default:
      return <Chip label={status} size="small" />;
  }
}

function formatDate(millis: number): string {
  return new Date(millis).toLocaleDateString('pl-PL', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

const FIELD_LABELS: Record<string, string> = {
  name: 'Nazwa',
  description: 'Opis',
  category: 'Kategoria',
  amenities: 'Udogodnienia',
  latitude: 'Szerokość geo.',
  longitude: 'Długość geo.',
  address: 'Adres',
};

function formatChangeValue(key: string, value: unknown): string {
  if (key === 'category') {
    return PLACE_CATEGORY_LABELS[value as PlaceCategory] || String(value);
  }
  if (key === 'amenities' && Array.isArray(value)) {
    return value.join(', ');
  }
  return String(value);
}

type SortField = 'createdAtMillis' | 'type';
type SortDir = 'asc' | 'desc';

export function ChangeRequestsPage() {
  const [requests, setRequests] = useState<PlaceChangeRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<ReportStatus | 'all'>('pending');
  const [typeFilter, setTypeFilter] = useState<string>('all');
  const [sortField, setSortField] = useState<SortField>('createdAtMillis');
  const [sortDir, setSortDir] = useState<SortDir>('desc');
  const [detailRequest, setDetailRequest] = useState<PlaceChangeRequest | null>(null);
  const [placeName, setPlaceName] = useState<string>('');
  const [confirmDialog, setConfirmDialog] = useState<{
    open: boolean;
    title: string;
    action: () => Promise<void>;
  }>({ open: false, title: '', action: async () => {} });

  useEffect(() => {
    fetchRequests();
  }, []);

  async function fetchRequests() {
    setLoading(true);
    try {
      const snap = await getDocs(
        query(collection(db, 'place_change_requests'), orderBy('createdAtMillis', 'desc'))
      );
      setRequests(snap.docs.map((d) => ({ id: d.id, ...d.data() } as PlaceChangeRequest)));
    } catch (err) {
      console.error('Failed to fetch change requests:', err);
    } finally {
      setLoading(false);
    }
  }

  function getFilteredRequests(): PlaceChangeRequest[] {
    let result = [...requests];

    // Status filter
    if (statusFilter !== 'all') {
      result = result.filter((r) => r.status === statusFilter);
    }

    // Type filter
    if (typeFilter !== 'all') {
      result = result.filter((r) => r.type === typeFilter);
    }

    // Sort
    result.sort((a, b) => {
      let aVal: any = (a as any)[sortField] ?? '';
      let bVal: any = (b as any)[sortField] ?? '';
      if (typeof aVal === 'string') aVal = aVal.toLowerCase();
      if (typeof bVal === 'string') bVal = bVal.toLowerCase();
      if (aVal < bVal) return sortDir === 'asc' ? -1 : 1;
      if (aVal > bVal) return sortDir === 'asc' ? 1 : -1;
      return 0;
    });

    return result;
  }

  function handleSort(field: SortField) {
    if (sortField === field) {
      setSortDir(sortDir === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDir('desc');
    }
  }

  async function openDetail(request: PlaceChangeRequest) {
    setDetailRequest(request);
    try {
      const placeDoc = await getDoc(doc(db, 'places', request.placeId));
      setPlaceName(placeDoc.exists() ? (placeDoc.data()?.name || 'Bez nazwy') : 'Miejsce usunięte');
    } catch {
      setPlaceName('Błąd pobierania');
    }
  }

  async function approveRequest(request: PlaceChangeRequest) {
    const placeRef = doc(db, 'places', request.placeId);
    const placeSnap = await getDoc(placeRef);

    if (placeSnap.exists()) {
      const updates: Record<string, unknown> = {};
      for (const [key, value] of Object.entries(request.changes)) {
        updates[key] = value;
      }
      await updateDoc(placeRef, updates);
    }

    await updateDoc(doc(db, 'place_change_requests', request.id), {
      status: 'resolved',
      resolvedAtMillis: Date.now(),
    });

    setDetailRequest(null);
    await fetchRequests();
  }

  async function dismissRequest(requestId: string) {
    await updateDoc(doc(db, 'place_change_requests', requestId), {
      status: 'dismissed',
      resolvedAtMillis: Date.now(),
    });
    setDetailRequest(null);
    await fetchRequests();
  }

  function confirm(title: string, action: () => Promise<void>) {
    setConfirmDialog({ open: true, title, action });
  }

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" py={6}>
        <CircularProgress />
      </Box>
    );
  }

  const pendingCount = requests.filter((r) => r.status === 'pending').length;
  const filteredRequests = getFilteredRequests();

  return (
    <Box>
      <Typography variant="h4" fontWeight={700} mb={1}>
        Propozycje zmian
      </Typography>
      <Typography variant="body2" color="text.secondary" mb={3}>
        Użytkownicy proponują korekty danych lub lokalizacji miejsc. Zatwierdź lub odrzuć.
      </Typography>

      <Box display="flex" gap={2} mb={3} flexWrap="wrap" alignItems="center">
        <ToggleButtonGroup
          value={statusFilter}
          exclusive
          onChange={(_, v) => v && setStatusFilter(v)}
          size="small"
        >
          <ToggleButton value="pending">Oczekujące ({pendingCount})</ToggleButton>
          <ToggleButton value="resolved">Zatwierdzone</ToggleButton>
          <ToggleButton value="dismissed">Odrzucone</ToggleButton>
          <ToggleButton value="all">Wszystkie</ToggleButton>
        </ToggleButtonGroup>

        <FormControl size="small" sx={{ minWidth: 140 }}>
          <InputLabel>Typ</InputLabel>
          <Select value={typeFilter} label="Typ" onChange={(e) => setTypeFilter(e.target.value)}>
            <MenuItem value="all">Wszystkie</MenuItem>
            <MenuItem value="EDIT">Dane</MenuItem>
            <MenuItem value="LOCATION">Lokalizacja</MenuItem>
          </Select>
        </FormControl>
      </Box>

      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>
                <TableSortLabel
                  active={sortField === 'createdAtMillis'}
                  direction={sortField === 'createdAtMillis' ? sortDir : 'desc'}
                  onClick={() => handleSort('createdAtMillis')}
                >
                  Data
                </TableSortLabel>
              </TableCell>
              <TableCell>
                <TableSortLabel
                  active={sortField === 'type'}
                  direction={sortField === 'type' ? sortDir : 'asc'}
                  onClick={() => handleSort('type')}
                >
                  Typ
                </TableSortLabel>
              </TableCell>
              <TableCell>Place ID</TableCell>
              <TableCell>Zmiany</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Akcje</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {filteredRequests.map((request) => (
              <TableRow key={request.id} hover>
                <TableCell>{formatDate(request.createdAtMillis)}</TableCell>
                <TableCell>
                  <Chip
                    label={request.type === 'LOCATION' ? 'Lokalizacja' : 'Dane'}
                    size="small"
                    variant="outlined"
                    color={request.type === 'LOCATION' ? 'info' : 'secondary'}
                  />
                </TableCell>
                <TableCell sx={{ fontFamily: 'monospace', fontSize: 12 }}>
                  {request.placeId.slice(0, 8)}...
                </TableCell>
                <TableCell>
                  {Object.keys(request.changes || {}).map((key) => (
                    <Chip
                      key={key}
                      label={FIELD_LABELS[key] || key}
                      size="small"
                      sx={{ mr: 0.5, mb: 0.5 }}
                    />
                  ))}
                </TableCell>
                <TableCell>{statusChip(request.status)}</TableCell>
                <TableCell>
                  <Tooltip title="Szczegóły">
                    <IconButton size="small" onClick={() => openDetail(request)}>
                      <VisibilityIcon />
                    </IconButton>
                  </Tooltip>
                  {request.status === 'pending' && (
                    <>
                      <Tooltip title="Zatwierdź zmiany">
                        <IconButton
                          color="success"
                          size="small"
                          onClick={() =>
                            confirm('Zatwierdzić i zastosować zmiany do miejsca?', () =>
                              approveRequest(request)
                            )
                          }
                        >
                          <CheckCircleIcon />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Odrzuć">
                        <IconButton
                          color="default"
                          size="small"
                          onClick={() =>
                            confirm('Odrzucić propozycję zmian?', () =>
                              dismissRequest(request.id)
                            )
                          }
                        >
                          <CancelIcon />
                        </IconButton>
                      </Tooltip>
                    </>
                  )}
                </TableCell>
              </TableRow>
            ))}
            {filteredRequests.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} align="center">
                  Brak propozycji zmian
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Detail Dialog */}
      <Dialog
        open={!!detailRequest}
        onClose={() => setDetailRequest(null)}
        maxWidth="sm"
        fullWidth
      >
        {detailRequest && (
          <>
            <DialogTitle>
              {detailRequest.type === 'LOCATION' ? 'Korekta lokalizacji' : 'Propozycja zmian'}
            </DialogTitle>
            <DialogContent dividers>
              <Box display="flex" flexDirection="column" gap={1.5}>
                <Typography variant="body2">
                  <strong>Miejsce:</strong> {placeName}
                </Typography>
                <Typography variant="body2">
                  <strong>Place ID:</strong>{' '}
                  <code style={{ fontSize: 12 }}>{detailRequest.placeId}</code>
                </Typography>
                <Typography variant="body2">
                  <strong>Zgłaszający (UID):</strong>{' '}
                  <code style={{ fontSize: 12 }}>{detailRequest.requesterId}</code>
                </Typography>
                <Typography variant="body2">
                  <strong>Data:</strong> {formatDate(detailRequest.createdAtMillis)}
                </Typography>

                <Typography variant="subtitle2" color="primary" mt={1}>
                  Proponowane zmiany:
                </Typography>
                <Table size="small">
                  <TableBody>
                    {Object.entries(detailRequest.changes || {}).map(([key, value]) => (
                      <TableRow key={key}>
                        <TableCell sx={{ fontWeight: 600, width: 140 }}>
                          {FIELD_LABELS[key] || key}
                        </TableCell>
                        <TableCell>{formatChangeValue(key, value)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </Box>
            </DialogContent>
            <DialogActions>
              {detailRequest.status === 'pending' && (
                <>
                  <Button
                    color="error"
                    onClick={() =>
                      confirm('Odrzucić propozycję zmian?', () =>
                        dismissRequest(detailRequest.id)
                      )
                    }
                  >
                    Odrzuć
                  </Button>
                  <Button
                    variant="contained"
                    color="success"
                    onClick={() =>
                      confirm('Zatwierdzić i zastosować zmiany?', () =>
                        approveRequest(detailRequest)
                      )
                    }
                  >
                    Zatwierdź
                  </Button>
                </>
              )}
              {detailRequest.status !== 'pending' && (
                <Button onClick={() => setDetailRequest(null)}>Zamknij</Button>
              )}
            </DialogActions>
          </>
        )}
      </Dialog>

      {/* Confirm Dialog */}
      <Dialog
        open={confirmDialog.open}
        onClose={() => setConfirmDialog((prev) => ({ ...prev, open: false }))}
      >
        <DialogTitle>Potwierdzenie</DialogTitle>
        <DialogContent>
          <Typography>{confirmDialog.title}</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmDialog((prev) => ({ ...prev, open: false }))}>
            Anuluj
          </Button>
          <Button
            variant="contained"
            color="primary"
            onClick={async () => {
              await confirmDialog.action();
              setConfirmDialog((prev) => ({ ...prev, open: false }));
            }}
          >
            Potwierdź
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
