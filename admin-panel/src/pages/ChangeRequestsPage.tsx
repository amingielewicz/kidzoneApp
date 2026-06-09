import { useEffect, useState, useRef } from 'react';
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
  TableSortLabel,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import VisibilityIcon from '@mui/icons-material/Visibility';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
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
    return (value as string[]).map((a) => AMENITY_LABELS[a] || a).join(', ');
  }
  return String(value);
}

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
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [confirmTitle, setConfirmTitle] = useState('');
  const confirmActionRef = useRef<(() => Promise<void>) | null>(null);

  function confirm(title: string, action: () => Promise<void>) {
    confirmActionRef.current = action;
    setConfirmTitle(title);
    setConfirmOpen(true);
  }

  async function handleConfirm() {
    if (confirmActionRef.current) await confirmActionRef.current();
    setConfirmOpen(false);
    confirmActionRef.current = null;
  }

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

  const [requesterEmail, setRequesterEmail] = useState('');

  async function openDetail(request: PlaceChangeRequest) {
    setDetailRequest(request);
    setRequesterEmail('');
    try {
      const [placeDoc, userDoc] = await Promise.all([
        getDoc(doc(db, 'places', request.placeId)),
        getDoc(doc(db, 'users', request.requesterId)),
      ]);
      setPlaceName(placeDoc.exists() ? (placeDoc.data()?.name || 'Bez nazwy') : 'Miejsce usunięte');
      if (userDoc.exists()) {
        setRequesterEmail(userDoc.data()?.email || '');
      }
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

  async function changeRequestStatus(requestId: string, newStatus: string) {
    const updates: any = { status: newStatus };
    if (newStatus === 'pending') {
      updates.resolvedAtMillis = null;
    } else {
      updates.resolvedAtMillis = Date.now();
    }
    await updateDoc(doc(db, 'place_change_requests', requestId), updates);
    setDetailRequest(null);
    await fetchRequests();
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

      <Box display="flex" gap={2} mb={3} flexWrap="wrap" alignItems="center">
        <Typography variant="body2" color="text.secondary" sx={{ flexGrow: 1 }}>
          Użytkownicy proponują korekty danych lub lokalizacji miejsc. Zatwierdź lub odrzuć.
        </Typography>

        <FormControl size="small" sx={{ minWidth: 150 }}>
          <InputLabel>Status</InputLabel>
          <Select value={statusFilter} label="Status" onChange={(e) => setStatusFilter(e.target.value as any)}>
            <MenuItem value="pending">Oczekujące ({pendingCount})</MenuItem>
            <MenuItem value="resolved">Zatwierdzone</MenuItem>
            <MenuItem value="dismissed">Odrzucone</MenuItem>
            <MenuItem value="all">Wszystkie</MenuItem>
          </Select>
        </FormControl>

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
                <TableCell>
                  <Box display="flex" alignItems="center" gap={0.5}>
                    <Tooltip title={request.placeId}><Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: 12 }}>{request.placeId.slice(0, 8)}...</Typography></Tooltip>
                    <Tooltip title="Kopiuj ID"><IconButton size="small" onClick={() => navigator.clipboard.writeText(request.placeId)}><ContentCopyIcon sx={{ fontSize: 14 }} /></IconButton></Tooltip>
                  </Box>
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
            <DialogTitle sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span>{detailRequest.type === 'LOCATION' ? 'Korekta lokalizacji' : 'Propozycja zmian'}</span>
              <IconButton size="small" onClick={() => setDetailRequest(null)}><CancelIcon /></IconButton>
            </DialogTitle>
            <DialogContent dividers>
              <Box display="flex" flexDirection="column" gap={1.5}>
                <Typography variant="body2">
                  <strong>Miejsce:</strong> {placeName}
                </Typography>
                <Box display="flex" alignItems="center" gap={0.5}>
                  <Typography variant="body2"><strong>Place ID:</strong> <code style={{ fontSize: 12 }}>{detailRequest.placeId}</code></Typography>
                  <Tooltip title="Kopiuj Place ID"><IconButton size="small" onClick={() => navigator.clipboard.writeText(detailRequest.placeId)}><ContentCopyIcon sx={{ fontSize: 14 }} /></IconButton></Tooltip>
                </Box>
                <Box>
                  <Typography variant="body2"><strong>Zgłaszający:</strong> {requesterEmail || '—'}</Typography>
                  <Box display="flex" alignItems="center" gap={0.5}>
                    <Typography variant="caption" color="text.secondary">UID: <code style={{ fontSize: 11 }}>{detailRequest.requesterId}</code></Typography>
                    <Tooltip title="Kopiuj UID"><IconButton size="small" onClick={() => navigator.clipboard.writeText(detailRequest.requesterId)}><ContentCopyIcon sx={{ fontSize: 14 }} /></IconButton></Tooltip>
                  </Box>
                </Box>
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
            <DialogActions sx={{ justifyContent: 'space-between', flexWrap: 'wrap', gap: 1, px: 3, py: 2 }}>
              <Box display="flex" gap={1} flexWrap="wrap">
                {detailRequest.status === 'pending' && (
                  <>
                    <Button size="small" color="success" variant="contained" onClick={() => confirm('Zatwierdzić i zastosować zmiany?', () => approveRequest(detailRequest))}>
                      Zatwierdź
                    </Button>
                    <Button size="small" variant="outlined" onClick={() => confirm('Odrzucić propozycję zmian?', () => dismissRequest(detailRequest.id))}>
                      Odrzuć
                    </Button>
                  </>
                )}
                {detailRequest.status === 'resolved' && (
                  <Button size="small" color="warning" variant="outlined" onClick={() => changeRequestStatus(detailRequest.id, 'pending')}>
                    Przywróć do oczekujących
                  </Button>
                )}
                {detailRequest.status === 'dismissed' && (
                  <>
                    <Button size="small" color="warning" variant="outlined" onClick={() => changeRequestStatus(detailRequest.id, 'pending')}>
                      Przywróć do oczekujących
                    </Button>
                    <Button size="small" color="success" variant="outlined" onClick={() => confirm('Zatwierdzić i zastosować zmiany?', () => approveRequest(detailRequest))}>
                      Zatwierdź
                    </Button>
                  </>
                )}
              </Box>
              <Button onClick={() => setDetailRequest(null)}>Zamknij</Button>
            </DialogActions>
          </>
        )}
      </Dialog>

      {/* Confirm Dialog */}
      <Dialog open={confirmOpen} onClose={() => setConfirmOpen(false)}>
        <DialogTitle>Potwierdzenie</DialogTitle>
        <DialogContent><Typography>{confirmTitle}</Typography></DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmOpen(false)}>Anuluj</Button>
          <Button variant="contained" color="primary" onClick={handleConfirm}>Potwierdź</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
