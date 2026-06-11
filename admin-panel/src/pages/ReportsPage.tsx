import { useEffect, useState, useRef } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  Box,
  Typography,
  Tabs,
  Tab,
  Chip,
  IconButton,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Tooltip,
  Rating,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material';
import TextField from '@mui/material/TextField';
import CancelIcon from '@mui/icons-material/Cancel';
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
import { adminFetch } from '../services/api';
import {
  PlaceReport,
  ReviewReport,
  PhotoReport,
  PLACE_REPORT_REASON_LABELS,
  REVIEW_REPORT_REASON_LABELS,
  PHOTO_REPORT_REASON_LABELS,
  PlaceReportReason,
  ReviewReportReason,
  PhotoReportReason,
  ReportStatus,
} from '../types';
import {
  PlaceReportsTable,
  ReviewReportsTable,
  PhotoReportsTable,
  formatDate,
  copyToClipboard,
} from './reports';

interface DetailInfo {
  placeName?: string;
  reporterName?: string;
  reporterEmail?: string;
  reviewComment?: string;
  reviewRating?: number;
  reviewAuthor?: string;
}

export function ReportsPage() {
  const [searchParams] = useSearchParams();
  const initialTab = parseInt(searchParams.get('tab') || '0') || 0;
  const [tab, setTab] = useState(initialTab);
  const [statusFilter, setStatusFilter] = useState<ReportStatus | 'all'>('pending');
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('desc');
  const [placeReports, setPlaceReports] = useState<PlaceReport[]>([]);
  const [reviewReports, setReviewReports] = useState<ReviewReport[]>([]);
  const [photoReports, setPhotoReports] = useState<PhotoReport[]>([]);
  const [placeNames, setPlaceNames] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [detailDialog, setDetailDialog] = useState<{
    open: boolean;
    type: 'place' | 'review' | 'photo';
    report: PlaceReport | ReviewReport | PhotoReport | null;
    info: DetailInfo;
    loadingInfo: boolean;
  }>({ open: false, type: 'place', report: null, info: {}, loadingInfo: false });
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [confirmTitle, setConfirmTitle] = useState('');
  const confirmActionRef = useRef<(() => Promise<void>) | null>(null);
  const [deleteDialog, setDeleteDialog] = useState<{
    open: boolean;
    title: string;
    action: (reason: string) => Promise<void>;
  }>({ open: false, title: '', action: async () => {} });
  const [deleteReason, setDeleteReason] = useState('');
  const [deleting, setDeleting] = useState(false);

  // ─── Helpers ───────────────────────────────────────────────────────────────

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

  function sortByDate<T extends { createdAtMillis: number }>(items: T[]): T[] {
    return [...items].sort((a, b) =>
      sortDir === 'desc'
        ? b.createdAtMillis - a.createdAtMillis
        : a.createdAtMillis - b.createdAtMillis,
    );
  }

  function filterByStatus<T extends { status: string }>(items: T[]): T[] {
    if (statusFilter === 'all') return items;
    return items.filter((i) => i.status === statusFilter);
  }

  function toggleSort() {
    setSortDir((prev) => (prev === 'asc' ? 'desc' : 'asc'));
  }

  // ─── Data fetching ─────────────────────────────────────────────────────────

  useEffect(() => {
    fetchAll();
  }, []);

  async function fetchAll() {
    setLoading(true);
    try {
      const [prSnap, rrSnap, phSnap] = await Promise.all([
        getDocs(query(collection(db, 'place_reports'), orderBy('createdAtMillis', 'desc'))),
        getDocs(query(collection(db, 'review_reports'), orderBy('createdAtMillis', 'desc'))),
        getDocs(query(collection(db, 'photo_reports'), orderBy('createdAtMillis', 'desc'))),
      ]);

      setPlaceReports(prSnap.docs.map((d) => ({ id: d.id, ...d.data() } as PlaceReport)));
      setReviewReports(rrSnap.docs.map((d) => ({ id: d.id, ...d.data() } as ReviewReport)));
      setPhotoReports(phSnap.docs.map((d) => ({ id: d.id, ...d.data() } as PhotoReport)));

      // Fetch place names for place reports
      const placeIds = [...new Set(prSnap.docs.map((d) => d.data().placeId).filter(Boolean))];
      const names: Record<string, string> = {};
      await Promise.all(
        placeIds.slice(0, 20).map(async (pid) => {
          try {
            const pDoc = await getDoc(doc(db, 'places', pid));
            names[pid] = pDoc.exists() ? (pDoc.data()?.name || 'Bez nazwy') : 'Usuniete';
          } catch {
            names[pid] = '\u2014';
          }
        }),
      );
      setPlaceNames(names);
    } catch (err) {
      console.error('Failed to fetch reports:', err);
    } finally {
      setLoading(false);
    }
  }

  // ─── Actions ───────────────────────────────────────────────────────────────

  async function resolveReport(collectionName: string, reportId: string) {
    await updateDoc(doc(db, collectionName, reportId), {
      status: 'resolved',
      resolvedAtMillis: Date.now(),
    });
    await fetchAll();
  }

  async function dismissReport(collectionName: string, reportId: string) {
    await updateDoc(doc(db, collectionName, reportId), {
      status: 'dismissed',
      resolvedAtMillis: Date.now(),
    });
    await fetchAll();
  }

  async function resolveAndDeletePlace(report: PlaceReport, reason: string) {
    const projectId = import.meta.env.VITE_FIREBASE_PROJECT_ID || 'playground-705e7162';
    const url = `https://us-central1-${projectId}.cloudfunctions.net/adminDeletePlace?placeId=${report.placeId}&reason=${encodeURIComponent(reason)}`;
    try {
      await adminFetch(url);
    } catch (e) {
      console.error(e);
    }
    await resolveReport('place_reports', report.id);
  }

  async function resolveAndDeleteReview(report: ReviewReport, reason: string) {
    const projectId = import.meta.env.VITE_FIREBASE_PROJECT_ID || 'playground-705e7162';
    const url = `https://us-central1-${projectId}.cloudfunctions.net/adminDeleteReview?reviewId=${report.reviewId}&reason=${encodeURIComponent(reason)}`;
    try {
      await adminFetch(url);
    } catch (e) {
      console.error('Failed to delete review:', e);
    }
    await resolveReport('review_reports', report.id);
  }

  async function deletePhotoViaCloudFunction(reportId: string, reason: string) {
    const projectId = import.meta.env.VITE_FIREBASE_PROJECT_ID || 'playground-705e7162';
    const url = `https://us-central1-${projectId}.cloudfunctions.net/adminDeletePhoto?reportId=${reportId}&reason=${encodeURIComponent(reason)}`;
    try {
      await adminFetch(url);
      await fetchAll();
    } catch (err) {
      console.error('Failed to delete photo via Cloud Function:', err);
      await resolveReport('photo_reports', reportId);
    }
  }

  async function changeReportStatus(newStatus: ReportStatus) {
    if (!detailDialog.report) return;
    const report = detailDialog.report;
    let collectionName = '';
    if (detailDialog.type === 'place') collectionName = 'place_reports';
    else if (detailDialog.type === 'review') collectionName = 'review_reports';
    else collectionName = 'photo_reports';

    const updates: any = { status: newStatus };
    if (newStatus === 'pending') {
      updates.resolvedAtMillis = null;
    } else {
      updates.resolvedAtMillis = Date.now();
    }
    await updateDoc(doc(db, collectionName, report.id), updates);
    setDetailDialog((p) => ({ ...p, open: false }));
    await fetchAll();
  }

  // ─── Detail dialogs ────────────────────────────────────────────────────────

  async function openDetailPlaceReport(report: PlaceReport) {
    setDetailDialog({ open: true, type: 'place', report, info: {}, loadingInfo: true });
    const info: DetailInfo = {};
    try {
      const [placeDoc, reporterDoc] = await Promise.all([
        getDoc(doc(db, 'places', report.placeId)),
        getDoc(doc(db, 'users', report.reporterId)),
      ]);
      info.placeName = placeDoc.exists() ? placeDoc.data()?.name || 'Bez nazwy' : 'Miejsce usuniete';
      if (reporterDoc.exists()) {
        info.reporterName = reporterDoc.data()?.name || '';
        info.reporterEmail = reporterDoc.data()?.email || '';
      }
    } catch {
      /* ignore */
    }
    setDetailDialog((prev) => ({ ...prev, info, loadingInfo: false }));
  }

  async function openDetailReviewReport(report: ReviewReport) {
    setDetailDialog({ open: true, type: 'review', report, info: {}, loadingInfo: true });
    const info: DetailInfo = {};
    try {
      const [reviewDoc, reporterDoc] = await Promise.all([
        getDoc(doc(db, 'reviews', report.reviewId)),
        getDoc(doc(db, 'users', report.reporterId)),
      ]);
      if (reviewDoc.exists()) {
        const rd = reviewDoc.data();
        info.reviewComment = rd?.comment || '';
        info.reviewRating = rd?.rating || 0;
        info.reviewAuthor = rd?.authorName || 'Anonim';
        if (rd?.placeId) {
          const placeDoc = await getDoc(doc(db, 'places', rd.placeId));
          info.placeName = placeDoc.exists()
            ? placeDoc.data()?.name || 'Bez nazwy'
            : 'Miejsce usuniete';
        }
      }
      if (reporterDoc.exists()) {
        info.reporterName = reporterDoc.data()?.name || '';
        info.reporterEmail = reporterDoc.data()?.email || '';
      }
    } catch {
      /* ignore */
    }
    setDetailDialog((prev) => ({ ...prev, info, loadingInfo: false }));
  }

  async function openDetailPhotoReport(report: PhotoReport) {
    setDetailDialog({ open: true, type: 'photo', report, info: {}, loadingInfo: true });
    const info: DetailInfo = {};
    try {
      const reporterDoc = await getDoc(doc(db, 'users', report.reporterId));
      if (reporterDoc.exists()) {
        info.reporterName = reporterDoc.data()?.name || '';
        info.reporterEmail = reporterDoc.data()?.email || '';
      }
    } catch {
      /* ignore */
    }
    setDetailDialog((prev) => ({ ...prev, info, loadingInfo: false }));
  }

  // ─── Render ────────────────────────────────────────────────────────────────

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" py={6}>
        <CircularProgress />
      </Box>
    );
  }

  const pendingPlaceCount = placeReports.filter((r) => r.status === 'pending').length;
  const pendingReviewCount = reviewReports.filter((r) => r.status === 'pending').length;
  const pendingPhotoCount = photoReports.filter((r) => r.status === 'pending').length;

  const filteredPlaceReports = sortByDate(filterByStatus(placeReports));
  const filteredReviewReports = sortByDate(filterByStatus(reviewReports));
  const filteredPhotoReports = sortByDate(filterByStatus(photoReports));

  return (
    <Box>
      <Typography variant="h4" fontWeight={700} mb={1}>
        Zgłoszenia
      </Typography>
      <Typography variant="body2" color="text.secondary" mb={3}>
        Przeglądaj zgłoszenia naruszeń od użytkowników. Rozwiąż lub odrzuć.
      </Typography>

      <Box display="flex" gap={2} mb={3} flexWrap="wrap" alignItems="center">
        <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ flexGrow: 1 }}>
          <Tab label={`Miejsca (${pendingPlaceCount})`} />
          <Tab label={`Opinie (${pendingReviewCount})`} />
          <Tab label={`Zdjecia (${pendingPhotoCount})`} />
        </Tabs>

        <FormControl size="small" sx={{ minWidth: 150 }}>
          <InputLabel>Status</InputLabel>
          <Select
            value={statusFilter}
            label="Status"
            onChange={(e) => setStatusFilter(e.target.value as ReportStatus | 'all')}
          >
            <MenuItem value="pending">Oczekujące</MenuItem>
            <MenuItem value="resolved">Rozwiązane</MenuItem>
            <MenuItem value="dismissed">Odrzucone</MenuItem>
            <MenuItem value="all">Wszystkie</MenuItem>
          </Select>
        </FormControl>
      </Box>

      {tab === 0 && (
        <PlaceReportsTable
          reports={filteredPlaceReports}
          placeNames={placeNames}
          sortDir={sortDir}
          onToggleSort={toggleSort}
          onViewDetail={openDetailPlaceReport}
          onDelete={(report) => {
            setDeleteReason('');
            setDeleteDialog({
              open: true,
              title: 'Podaj powód usunięcia miejsca:',
              action: async (reason) => {
                await resolveAndDeletePlace(report, reason);
              },
            });
          }}
          onDismiss={(report) =>
            confirm('Odrzucić?', () => dismissReport('place_reports', report.id))
          }
        />
      )}

      {tab === 1 && (
        <ReviewReportsTable
          reports={filteredReviewReports}
          sortDir={sortDir}
          onToggleSort={toggleSort}
          onViewDetail={openDetailReviewReport}
          onDelete={(report) => {
            setDeleteReason('');
            setDeleteDialog({
              open: true,
              title: 'Podaj powód usunięcia opinii:',
              action: async (reason) => {
                await resolveAndDeleteReview(report, reason);
              },
            });
          }}
          onDismiss={(report) =>
            confirm('Odrzucić?', () => dismissReport('review_reports', report.id))
          }
        />
      )}

      {tab === 2 && (
        <PhotoReportsTable
          reports={filteredPhotoReports}
          sortDir={sortDir}
          onToggleSort={toggleSort}
          onViewDetail={openDetailPhotoReport}
          onDelete={(report) => {
            setDeleteReason('');
            setDeleteDialog({
              open: true,
              title: 'Podaj powód usunięcia zdjęcia:',
              action: async (reason) => {
                await deletePhotoViaCloudFunction(report.id, reason);
              },
            });
          }}
          onDismiss={(report) =>
            confirm('Odrzucić?', () => dismissReport('photo_reports', report.id))
          }
        />
      )}

      {/* Detail Dialog */}
      <Dialog
        open={detailDialog.open}
        onClose={() => setDetailDialog((p) => ({ ...p, open: false }))}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span>
            {detailDialog.type === 'place' && 'Szczegóły zgłoszenia miejsca'}
            {detailDialog.type === 'review' && 'Szczegóły zgłoszenia opinii'}
            {detailDialog.type === 'photo' && 'Szczegóły zgłoszenia zdjęcia'}
          </span>
          <IconButton size="small" onClick={() => setDetailDialog((p) => ({ ...p, open: false }))}>
            <CancelIcon />
          </IconButton>
        </DialogTitle>
        <DialogContent dividers>
          {detailDialog.loadingInfo ? (
            <Box display="flex" justifyContent="center" py={3}>
              <CircularProgress size={24} />
            </Box>
          ) : (
            <Box display="flex" flexDirection="column" gap={1.5}>
              <Typography variant="subtitle2" color="primary">
                Zgłaszający:
              </Typography>
              <Typography variant="body2">
                {detailDialog.info.reporterName || 'Nieznany'}{' '}
                {detailDialog.info.reporterEmail && `(${detailDialog.info.reporterEmail})`}
              </Typography>

              {detailDialog.type === 'place' && detailDialog.report && (
                <>
                  <Typography variant="subtitle2" color="primary" mt={1}>
                    Zgłoszone miejsce:
                  </Typography>
                  <Typography variant="body2">
                    <strong>Nazwa:</strong> {detailDialog.info.placeName || '\u2014'}
                  </Typography>
                  <Box display="flex" alignItems="center" gap={0.5}>
                    <Typography variant="body2">
                      <strong>Place ID:</strong>{' '}
                      <code>{(detailDialog.report as PlaceReport).placeId}</code>
                    </Typography>
                    <Tooltip title="Kopiuj">
                      <IconButton
                        size="small"
                        onClick={() =>
                          copyToClipboard((detailDialog.report as PlaceReport).placeId)
                        }
                      >
                        <ContentCopyIcon sx={{ fontSize: 14 }} />
                      </IconButton>
                    </Tooltip>
                  </Box>
                  <Typography variant="body2">
                    <strong>Powód:</strong>{' '}
                    {
                      PLACE_REPORT_REASON_LABELS[
                        (detailDialog.report as PlaceReport).reason as PlaceReportReason
                      ]
                    }
                  </Typography>
                  <Typography variant="body2">
                    <strong>Komentarz:</strong>{' '}
                    {(detailDialog.report as PlaceReport).comment || '(brak)'}
                  </Typography>
                </>
              )}

              {detailDialog.type === 'review' && detailDialog.report && (
                <>
                  <Typography variant="subtitle2" color="primary" mt={1}>
                    Zgłoszona opinia:
                  </Typography>
                  {detailDialog.info.placeName && (
                    <Typography variant="body2">
                      <strong>Miejsce:</strong> {detailDialog.info.placeName}
                    </Typography>
                  )}
                  <Box display="flex" alignItems="center" gap={0.5}>
                    <Typography variant="body2">
                      <strong>Review ID:</strong>{' '}
                      <code>{(detailDialog.report as ReviewReport).reviewId}</code>
                    </Typography>
                    <Tooltip title="Kopiuj">
                      <IconButton
                        size="small"
                        onClick={() =>
                          copyToClipboard((detailDialog.report as ReviewReport).reviewId)
                        }
                      >
                        <ContentCopyIcon sx={{ fontSize: 14 }} />
                      </IconButton>
                    </Tooltip>
                  </Box>
                  <Typography variant="body2">
                    <strong>Autor:</strong> {detailDialog.info.reviewAuthor || '\u2014'}
                  </Typography>
                  {detailDialog.info.reviewRating != null && (
                    <Box display="flex" alignItems="center" gap={1}>
                      <strong>Ocena:</strong>
                      <Rating value={detailDialog.info.reviewRating} size="small" readOnly />
                    </Box>
                  )}
                  <Typography variant="body2">
                    <strong>Treść:</strong> {detailDialog.info.reviewComment || '(brak)'}
                  </Typography>
                  <Typography variant="body2">
                    <strong>Powód:</strong>{' '}
                    {
                      REVIEW_REPORT_REASON_LABELS[
                        (detailDialog.report as ReviewReport).reason as ReviewReportReason
                      ]
                    }
                  </Typography>
                </>
              )}

              {detailDialog.type === 'photo' && detailDialog.report && (
                <>
                  <Typography variant="subtitle2" color="primary" mt={1}>
                    Zgłoszone zdjęcie:
                  </Typography>
                  {(detailDialog.report as PhotoReport).photoUrl ? (
                    <Box textAlign="center">
                      <img
                        src={(detailDialog.report as PhotoReport).photoUrl}
                        alt="Zdjecie"
                        style={{
                          maxWidth: '100%',
                          maxHeight: 300,
                          borderRadius: 8,
                          objectFit: 'contain',
                        }}
                      />
                    </Box>
                  ) : (
                    <Chip
                      label="Zdjęcie zostało usunięte — zgłoszenie nieaktualne"
                      color="default"
                    />
                  )}
                  <Typography variant="body2">
                    <strong>Powód:</strong>{' '}
                    {
                      PHOTO_REPORT_REASON_LABELS[
                        (detailDialog.report as PhotoReport).reason as PhotoReportReason
                      ]
                    }
                  </Typography>
                  <Typography variant="body2">
                    <strong>Komentarz:</strong>{' '}
                    {(detailDialog.report as PhotoReport).comment || '(brak)'}
                  </Typography>
                </>
              )}

              <Typography variant="body2" mt={1}>
                <strong>Data:</strong>{' '}
                {detailDialog.report && formatDate(detailDialog.report.createdAtMillis)}
              </Typography>
              <Typography variant="body2">
                <strong>Status:</strong> {detailDialog.report?.status}
              </Typography>
            </Box>
          )}
        </DialogContent>
        <DialogActions
          sx={{ justifyContent: 'space-between', flexWrap: 'wrap', gap: 1, px: 3, py: 2 }}
        >
          <Box display="flex" gap={1} flexWrap="wrap">
            {detailDialog.report && detailDialog.report.status !== 'pending' && (
              <Button
                size="small"
                color="warning"
                variant="outlined"
                onClick={() => changeReportStatus('pending')}
              >
                Przywróć do oczekujących
              </Button>
            )}
            {detailDialog.report && detailDialog.report.status !== 'resolved' && (
              <Button
                size="small"
                color="success"
                variant="outlined"
                onClick={() => changeReportStatus('resolved')}
              >
                Oznacz jako rozwiązane
              </Button>
            )}
            {detailDialog.report && detailDialog.report.status !== 'dismissed' && (
              <Button size="small" variant="outlined" onClick={() => changeReportStatus('dismissed')}>
                Odrzuć
              </Button>
            )}
          </Box>
          <Button onClick={() => setDetailDialog((p) => ({ ...p, open: false }))}>Zamknij</Button>
        </DialogActions>
      </Dialog>

      {/* Delete with Reason Dialog */}
      <Dialog
        open={deleteDialog.open}
        onClose={() => setDeleteDialog((prev) => ({ ...prev, open: false }))}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>{deleteDialog.title}</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            fullWidth
            multiline
            rows={3}
            label="Powód usunięcia"
            value={deleteReason}
            onChange={(e) => setDeleteReason(e.target.value)}
            placeholder="Wpisz powód usunięcia (zostanie wysłany do użytkownika)"
            sx={{ mt: 1 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteDialog((prev) => ({ ...prev, open: false }))}>
            Anuluj
          </Button>
          <Button
            variant="contained"
            color="error"
            disabled={!deleteReason.trim() || deleting}
            onClick={async () => {
              setDeleting(true);
              await deleteDialog.action(deleteReason);
              setDeleting(false);
              setDeleteDialog((prev) => ({ ...prev, open: false }));
            }}
          >
            {deleting ? <CircularProgress size={20} color="inherit" /> : 'Usuń'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Confirm Dialog */}
      <Dialog open={confirmOpen} onClose={() => setConfirmOpen(false)}>
        <DialogTitle>Potwierdzenie</DialogTitle>
        <DialogContent>
          <Typography>{confirmTitle}</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmOpen(false)}>Anuluj</Button>
          <Button variant="contained" color="error" onClick={handleConfirm}>
            Potwierdź
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
