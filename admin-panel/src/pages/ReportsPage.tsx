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
} from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import DeleteIcon from '@mui/icons-material/Delete';
import CancelIcon from '@mui/icons-material/Cancel';
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

export function ReportsPage() {
  const [tab, setTab] = useState(0);
  const [placeReports, setPlaceReports] = useState<PlaceReport[]>([]);
  const [reviewReports, setReviewReports] = useState<ReviewReport[]>([]);
  const [photoReports, setPhotoReports] = useState<PhotoReport[]>([]);
  const [loading, setLoading] = useState(true);
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

      setPlaceReports(
        prSnap.docs.map((d) => ({ id: d.id, ...d.data() } as PlaceReport))
      );
      setReviewReports(
        rrSnap.docs.map((d) => ({ id: d.id, ...d.data() } as ReviewReport))
      );
      setPhotoReports(
        phSnap.docs.map((d) => ({ id: d.id, ...d.data() } as PhotoReport))
      );
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
    // Usuń miejsce
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

  const pendingPlaceCount = placeReports.filter((r) => r.status === 'pending').length;
  const pendingReviewCount = reviewReports.filter((r) => r.status === 'pending').length;
  const pendingPhotoCount = photoReports.filter((r) => r.status === 'pending').length;

  return (
    <Box>
      <Typography variant="h4" fontWeight={700} mb={3}>
        Zgłoszenia
      </Typography>

      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 3 }}>
        <Tab label={`Miejsca (${pendingPlaceCount})`} />
        <Tab label={`Opinie (${pendingReviewCount})`} />
        <Tab label={`Zdjęcia (${pendingPhotoCount})`} />
      </Tabs>

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
              {placeReports.map((report) => (
                <TableRow key={report.id}>
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
                    {report.status !== 'pending' && (
                      <CheckCircleIcon color="disabled" fontSize="small" />
                    )}
                  </TableCell>
                </TableRow>
              ))}
              {placeReports.length === 0 && (
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
              {reviewReports.map((report) => (
                <TableRow key={report.id}>
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
                    {report.status !== 'pending' && (
                      <CheckCircleIcon color="disabled" fontSize="small" />
                    )}
                  </TableCell>
                </TableRow>
              ))}
              {reviewReports.length === 0 && (
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
              {photoReports.map((report) => (
                <TableRow key={report.id}>
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
                    {report.status === 'pending' && (
                      <>
                        <Tooltip title="Rozwiąż (zdjęcie usunięte)">
                          <IconButton
                            color="success"
                            size="small"
                            onClick={() =>
                              confirm('Oznaczyć jako rozwiązane?', () =>
                                resolveReport('photo_reports', report.id)
                              )
                            }
                          >
                            <CheckCircleIcon />
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
                    {report.status !== 'pending' && (
                      <CheckCircleIcon color="disabled" fontSize="small" />
                    )}
                  </TableCell>
                </TableRow>
              ))}
              {photoReports.length === 0 && (
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
