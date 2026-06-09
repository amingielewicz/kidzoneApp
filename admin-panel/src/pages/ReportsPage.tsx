import { useEffect, useState } from 'react';
import {
  Box,
  Typography,
  Tabs,
  Tab,
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
  Rating,
} from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import DeleteIcon from '@mui/icons-material/Delete';
import CancelIcon from '@mui/icons-material/Cancel';
import VisibilityIcon from '@mui/icons-material/Visibility';
import {
  collection,
  query,
  orderBy,
  getDocs,
  doc,
  updateDoc,
  deleteDoc,
  getDoc,
} from 'firebase/firestore';
import { db } from '../services/firebase';
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

function statusChip(status: string) {
  switch (status) {
    case 'pending':
      return <Chip label="Oczekuje" color="warning" size="small" />;
    case 'resolved':
      return <Chip label="Rozwiązane" color="success" size="small" />;
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

interface DetailInfo {
  placeName?: string;
  reporterName?: string;
  reporterEmail?: string;
  reviewComment?: string;
  reviewRating?: number;
  reviewAuthor?: string;
}

export function ReportsPage() {
  const [tab, setTab] = useState(0);
  const [statusFilter, setStatusFilter] = useState<ReportStatus | 'all'>('pending');
  const [placeReports, setPlaceReports] = useState<PlaceReport[]>([]);
  const [reviewReports, setReviewReports] = useState<ReviewReport[]>([]);
  const [photoReports, setPhotoReports] = useState<PhotoReport[]>([]);
  const [loading, setLoading] = useState(true);
  const [detailDialog, setDetailDialog] = useState<{
    open: boolean;
    type: 'place' | 'review' | 'photo';
    report: PlaceReport | ReviewReport | PhotoReport | null;
    info: DetailInfo;
    loadingInfo: boolean;
  }>({ open: false, type: 'place', report: null, info: {}, loadingInfo: false });
  const [confirmDialog, setConfirmDialog] = useState<{
    open: boolean;
    title: string;
    action: () => Promise<void>;
  }>({ open: false, title: '', action: async () => {} });

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
    } catch (err) {
      console.error('Failed to fetch reports:', err);
    } finally {
      setLoading(false);
    }
  }

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

  async function resolveAndDeletePlace(report: PlaceReport) {
    const placeRef = doc(db, 'places', report.placeId);
    const placeSnap = await getDoc(placeRef);
    if (placeSnap.exists()) {
      await deleteDoc(placeRef);
    }
    await resolveReport('place_reports', report.id);
  }

  async function resolveAndDeleteReview(report: ReviewReport) {
    const reviewRef = doc(db, 'reviews', report.reviewId);
    const reviewSnap = await getDoc(reviewRef);
    if (reviewSnap.exists()) {
      await deleteDoc(reviewRef);
    }
    await resolveReport('review_reports', report.id);
  }

  async function deletePhotoViaCloudFunction(reportId: string) {
    const projectId = import.meta.env.VITE_FIREBASE_PROJECT_ID || 'playground-705e7162';
    const url = `https://us-central1-${projectId}.cloudfunctions.net/adminDeletePhoto?reportId=${reportId}`;
    try {
      await fetch(url);
      await fetchAll();
    } catch (err) {
      console.error('Failed to delete photo via Cloud Function:', err);
      // Fallback: oznacz jako resolved ręcznie
      await resolveReport('photo_reports', reportId);
    }
  }

  async function openDetailPlaceReport(report: PlaceReport) {
    setDetailDialog({ open: true, type: 'place', report, info: {}, loadingInfo: true });
    const info: DetailInfo = {};
    try {
      const [placeDoc, reporterDoc] = await Promise.all([
        getDoc(doc(db, 'places', report.placeId)),
        getDoc(doc(db, 'users', report.reporterId)),
      ]);
      info.placeName = placeDoc.exists() ? placeDoc.data()?.name || 'Bez nazwy' : 'Miejsce usunięte';
      if (reporterDoc.exists()) {
        info.reporterName = reporterDoc.data()?.name || '';
        info.reporterEmail = reporterDoc.data()?.email || '';
      }
    } catch { /* ignore */ }
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
          info.placeName = placeDoc.exists() ? placeDoc.data()?.name || 'Bez nazwy' : 'Miejsce usunięte';
        }
      }
      if (reporterDoc.exists()) {
        info.reporterName = reporterDoc.data()?.name || '';
        info.reporterEmail = reporterDoc.data()?.email || '';
      }
    } catch { /* ignore */ }
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
    } catch { /* ignore */ }
    setDetailDialog((prev) => ({ ...prev, info, loadingInfo: false }));
  }

  function confirm(title: string, action: () => Promise<void>) {
    setConfirmDialog({ open: true, title, action });
  }

  function filterByStatus<T extends { status: string }>(items: T[]): T[] {
    if (statusFilter === 'all') return items;
    return items.filter((i) => i.status === statusFilter);
  }

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

  const filteredPlaceReports = filterByStatus(placeReports);
  const filteredReviewReports = filterByStatus(reviewReports);
  const filteredPhotoReports = filterByStatus(photoReports);

  return (
    <Box>
      <Typography variant="h4" fontWeight={700} mb={3}>
        Zgłoszenia
      </Typography>

      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3} flexWrap="wrap" gap={2}>
        <Tabs value={tab} onChange={(_, v) => setTab(v)}>
          <Tab label={`Miejsca (${pendingPlaceCount})`} />
          <Tab label={`Opinie (${pendingReviewCount})`} />
          <Tab label={`Zdjęcia (${pendingPhotoCount})`} />
        </Tabs>

        <ToggleButtonGroup
          value={statusFilter}
          exclusive
          onChange={(_, v) => v && setStatusFilter(v)}
          size="small"
        >
          <ToggleButton value="pending">Oczekujące</ToggleButton>
          <ToggleButton value="resolved">Rozwiązane</ToggleButton>
          <ToggleButton value="dismissed">Odrzucone</ToggleButton>
          <ToggleButton value="all">Wszystkie</ToggleButton>
        </ToggleButtonGroup>
      </Box>

      {tab === 0 && (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Data</TableCell>
                <TableCell>Place ID</TableCell>
                <TableCell>Powód</TableCell>
                <TableCell>Komentarz</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Akcje</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredPlaceReports.map((report) => (
                <TableRow key={report.id} hover>
                  <TableCell>{formatDate(report.createdAtMillis)}</TableCell>
                  <TableCell sx={{ fontFamily: 'monospace', fontSize: 12 }}>
                    {report.placeId.slice(0, 8)}...
                  </TableCell>
                  <TableCell>
                    {PLACE_REPORT_REASON_LABELS[report.reason as PlaceReportReason] || report.reason}
                  </TableCell>
                  <TableCell sx={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {report.comment || '—'}
                  </TableCell>
                  <TableCell>{statusChip(report.status)}</TableCell>
                  <TableCell>
                    <Tooltip title="Szczegóły">
                      <IconButton size="small" onClick={() => openDetailPlaceReport(report)}>
                        <VisibilityIcon />
                      </IconButton>
                    </Tooltip>
                    {report.status === 'pending' && (
                      <>
                        <Tooltip title="Usuń miejsce i rozwiąż">
                          <IconButton
                            color="error"
                            size="small"
                            onClick={() =>
                              confirm('Usunąć miejsce i rozwiązać zgłoszenie?', () =>
                                resolveAndDeletePlace(report)
                              )
                            }
                          >
                            <DeleteIcon />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Odrzuć zgłoszenie">
                          <IconButton
                            color="default"
                            size="small"
                            onClick={() =>
                              confirm('Odrzucić zgłoszenie?', () =>
                                dismissReport('place_reports', report.id)
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
              {filteredPlaceReports.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6} align="center">
                    Brak zgłoszeń
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {tab === 1 && (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Data</TableCell>
                <TableCell>Review ID</TableCell>
                <TableCell>Powód</TableCell>
                <TableCell>Komentarz</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Akcje</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredReviewReports.map((report) => (
                <TableRow key={report.id} hover>
                  <TableCell>{formatDate(report.createdAtMillis)}</TableCell>
                  <TableCell sx={{ fontFamily: 'monospace', fontSize: 12 }}>
                    {report.reviewId.slice(0, 8)}...
                  </TableCell>
                  <TableCell>
                    {REVIEW_REPORT_REASON_LABELS[report.reason as ReviewReportReason] || report.reason}
                  </TableCell>
                  <TableCell sx={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {report.comment || '—'}
                  </TableCell>
                  <TableCell>{statusChip(report.status)}</TableCell>
                  <TableCell>
                    <Tooltip title="Szczegóły">
                      <IconButton size="small" onClick={() => openDetailReviewReport(report)}>
                        <VisibilityIcon />
                      </IconButton>
                    </Tooltip>
                    {report.status === 'pending' && (
                      <>
                        <Tooltip title="Usuń opinię i rozwiąż">
                          <IconButton
                            color="error"
                            size="small"
                            onClick={() =>
                              confirm('Usunąć opinię i rozwiązać zgłoszenie?', () =>
                                resolveAndDeleteReview(report)
                              )
                            }
                          >
                            <DeleteIcon />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Odrzuć zgłoszenie">
                          <IconButton
                            color="default"
                            size="small"
                            onClick={() =>
                              confirm('Odrzucić zgłoszenie?', () =>
                                dismissReport('review_reports', report.id)
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
              {filteredReviewReports.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6} align="center">
                    Brak zgłoszeń
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {tab === 2 && (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Data</TableCell>
                <TableCell>Zdjęcie</TableCell>
                <TableCell>Powód</TableCell>
                <TableCell>Komentarz</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Akcje</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredPhotoReports.map((report) => (
                <TableRow key={report.id} hover>
                  <TableCell>{formatDate(report.createdAtMillis)}</TableCell>
                  <TableCell>
                    {report.photoUrl && (
                      <a href={report.photoUrl} target="_blank" rel="noopener noreferrer">
                        <img
                          src={report.photoUrl}
                          alt="Zgłoszone zdjęcie"
                          style={{
                            width: 60,
                            height: 60,
                            objectFit: 'cover',
                            borderRadius: 4,
                          }}
                        />
                      </a>
                    )}
                  </TableCell>
                  <TableCell>
                    {PHOTO_REPORT_REASON_LABELS[report.reason as PhotoReportReason] || report.reason}
                  </TableCell>
                  <TableCell sx={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {report.comment || '—'}
                  </TableCell>
                  <TableCell>{statusChip(report.status)}</TableCell>
                  <TableCell>
                    <Tooltip title="Szczegóły">
                      <IconButton size="small" onClick={() => openDetailPhotoReport(report)}>
                        <VisibilityIcon />
                      </IconButton>
                    </Tooltip>
                    {report.status === 'pending' && (
                      <>
                        <Tooltip title="Usuń zdjęcie (Cloud Function)">
                          <IconButton
                            color="error"
                            size="small"
                            onClick={() =>
                              confirm('Usunąć zdjęcie ze Storage i wszystkich dokumentów?', () =>
                                deletePhotoViaCloudFunction(report.id)
                              )
                            }
                          >
                            <DeleteIcon />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Odrzuć zgłoszenie">
                          <IconButton
                            color="default"
                            size="small"
                            onClick={() =>
                              confirm('Odrzucić zgłoszenie?', () =>
                                dismissReport('photo_reports', report.id)
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
              {filteredPhotoReports.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6} align="center">
                    Brak zgłoszeń
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {/* Detail Dialog */}
      <Dialog
        open={detailDialog.open}
        onClose={() => setDetailDialog((prev) => ({ ...prev, open: false }))}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>
          {detailDialog.type === 'place' && 'Szczegóły zgłoszenia miejsca'}
          {detailDialog.type === 'review' && 'Szczegóły zgłoszenia opinii'}
          {detailDialog.type === 'photo' && 'Szczegóły zgłoszenia zdjęcia'}
        </DialogTitle>
        <DialogContent dividers>
          {detailDialog.loadingInfo ? (
            <Box display="flex" justifyContent="center" py={3}>
              <CircularProgress size={24} />
            </Box>
          ) : (
            <Box display="flex" flexDirection="column" gap={1.5}>
              {/* Zgłaszający */}
              <Typography variant="subtitle2" color="primary">
                Zgłaszający:
              </Typography>
              <Typography variant="body2">
                {detailDialog.info.reporterName || 'Nieznany'}{' '}
                {detailDialog.info.reporterEmail && `(${detailDialog.info.reporterEmail})`}
              </Typography>

              {/* Place Report details */}
              {detailDialog.type === 'place' && detailDialog.report && (
                <>
                  <Typography variant="subtitle2" color="primary" mt={1}>
                    Zgłoszone miejsce:
                  </Typography>
                  <Typography variant="body2">
                    <strong>Nazwa:</strong> {detailDialog.info.placeName || '—'}
                  </Typography>
                  <Typography variant="body2">
                    <strong>Place ID:</strong>{' '}
                    <code>{(detailDialog.report as PlaceReport).placeId}</code>
                  </Typography>
                  <Typography variant="body2">
                    <strong>Powód:</strong>{' '}
                    {PLACE_REPORT_REASON_LABELS[(detailDialog.report as PlaceReport).reason as PlaceReportReason] ||
                      (detailDialog.report as PlaceReport).reason}
                  </Typography>
                  <Typography variant="body2">
                    <strong>Komentarz:</strong>{' '}
                    {(detailDialog.report as PlaceReport).comment || '(brak)'}
                  </Typography>
                </>
              )}

              {/* Review Report details */}
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
                  <Typography variant="body2">
                    <strong>Autor opinii:</strong> {detailDialog.info.reviewAuthor || '—'}
                  </Typography>
                  {detailDialog.info.reviewRating != null && (
                    <Box display="flex" alignItems="center" gap={1}>
                      <strong>Ocena:</strong>
                      <Rating value={detailDialog.info.reviewRating} size="small" readOnly />
                    </Box>
                  )}
                  <Typography variant="body2">
                    <strong>Treść opinii:</strong> {detailDialog.info.reviewComment || '(brak)'}
                  </Typography>
                  <Typography variant="body2">
                    <strong>Powód zgłoszenia:</strong>{' '}
                    {REVIEW_REPORT_REASON_LABELS[(detailDialog.report as ReviewReport).reason as ReviewReportReason] ||
                      (detailDialog.report as ReviewReport).reason}
                  </Typography>
                  <Typography variant="body2">
                    <strong>Komentarz zgłaszającego:</strong>{' '}
                    {(detailDialog.report as ReviewReport).comment || '(brak)'}
                  </Typography>
                </>
              )}

              {/* Photo Report details */}
              {detailDialog.type === 'photo' && detailDialog.report && (
                <>
                  <Typography variant="subtitle2" color="primary" mt={1}>
                    Zgłoszone zdjęcie:
                  </Typography>
                  {(detailDialog.report as PhotoReport).photoUrl && (
                    <Box textAlign="center">
                      <a
                        href={(detailDialog.report as PhotoReport).photoUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        <img
                          src={(detailDialog.report as PhotoReport).photoUrl}
                          alt="Zgłoszone zdjęcie"
                          style={{
                            maxWidth: '100%',
                            maxHeight: 300,
                            borderRadius: 8,
                            objectFit: 'contain',
                          }}
                        />
                      </a>
                    </Box>
                  )}
                  <Typography variant="body2">
                    <strong>Powód:</strong>{' '}
                    {PHOTO_REPORT_REASON_LABELS[(detailDialog.report as PhotoReport).reason as PhotoReportReason] ||
                      (detailDialog.report as PhotoReport).reason}
                  </Typography>
                  <Typography variant="body2">
                    <strong>Komentarz:</strong>{' '}
                    {(detailDialog.report as PhotoReport).comment || '(brak)'}
                  </Typography>
                </>
              )}

              <Typography variant="body2" mt={1}>
                <strong>Data zgłoszenia:</strong>{' '}
                {detailDialog.report && formatDate(detailDialog.report.createdAtMillis)}
              </Typography>
              <Typography variant="body2">
                <strong>Status:</strong> {detailDialog.report?.status}
              </Typography>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDetailDialog((prev) => ({ ...prev, open: false }))}>
            Zamknij
          </Button>
        </DialogActions>
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
            color="error"
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
