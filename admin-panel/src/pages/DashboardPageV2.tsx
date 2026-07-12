import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Avatar,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Grid,
  List,
  ListItem,
  ListItemAvatar,
  ListItemButton,
  ListItemText,
  Paper,
  Rating,
  Typography,
} from '@mui/material';
import PlaceIcon from '@mui/icons-material/Place';
import ReviewsIcon from '@mui/icons-material/RateReview';
import PeopleIcon from '@mui/icons-material/People';
import ReportIcon from '@mui/icons-material/Report';
import WarningIcon from '@mui/icons-material/Warning';
import PhotoIcon from '@mui/icons-material/Photo';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import NewReleasesIcon from '@mui/icons-material/NewReleases';
import {
  collection,
  getCountFromServer,
  getDocs,
  limit,
  orderBy,
  query,
  where,
} from 'firebase/firestore';
import { db } from '../services/firebase';
import {
  clearDashboardReports,
  getDashboardReportsClearedAt,
  REPORTS_CLEARED_EVENT,
} from '../services/reportNotifications';

interface RecentUser {
  id: string;
  name: string;
  email: string;
  avatarUrl?: string;
  createdAtMillis: number;
}

interface RecentPlace {
  id: string;
  name: string;
  averageRating: number;
  createdAtMillis: number;
}

interface RecentReview {
  id: string;
  authorName: string;
  comment: string;
  rating: number;
  createdAtMillis: number;
}

interface RecentReport {
  id: string;
  type: 'place' | 'review' | 'photo';
  reason: string;
  comment: string;
  createdAtMillis: number;
}

const REASON_LABELS: Record<string, string> = {
  NOT_EXISTS: 'Nie istnieje / zamknięte',
  INAPPROPRIATE: 'Nieodpowiednia treść',
  DUPLICATE: 'Duplikat',
  FALSE_DATA: 'Fałszywe dane',
  SPAM: 'Spam / reklama',
  OFFENSIVE: 'Obraźliwa treść',
  FALSE_INFO: 'Fałszywe informacje',
  NOT_RELEVANT: 'Nie dotyczy miejsca',
  COPYRIGHT: 'Prawa autorskie',
  OTHER: 'Inne',
};

function formatDate(millis: number): string {
  return new Date(millis).toLocaleDateString('pl-PL', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
}

export function DashboardPage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({ places: 0, reviews: 0, users: 0, pendingReports: 0 });
  const [recentUsers, setRecentUsers] = useState<RecentUser[]>([]);
  const [recentPlaces, setRecentPlaces] = useState<RecentPlace[]>([]);
  const [recentReviews, setRecentReviews] = useState<RecentReview[]>([]);
  const [recentReports, setRecentReports] = useState<RecentReport[]>([]);
  const [newReportsCount, setNewReportsCount] = useState(0);

  const fetchDashboard = useCallback(async () => {
    setLoading(true);
    try {
      const [placesC, reviewsC, usersC, usersSnap, placesSnap, reviewsSnap] = await Promise.all([
        getCountFromServer(collection(db, 'places')),
        getCountFromServer(collection(db, 'reviews')),
        getCountFromServer(collection(db, 'users')),
        getDocs(query(collection(db, 'users'), orderBy('createdAtMillis', 'desc'), limit(5))),
        getDocs(query(collection(db, 'places'), orderBy('createdAtMillis', 'desc'), limit(5))),
        getDocs(query(collection(db, 'reviews'), orderBy('createdAtMillis', 'desc'), limit(5))),
      ]);

      setRecentUsers(usersSnap.docs.map((d) => ({ id: d.id, ...d.data() }) as RecentUser));
      setRecentPlaces(placesSnap.docs.map((d) => ({ id: d.id, ...d.data() }) as RecentPlace));
      setRecentReviews(reviewsSnap.docs.map((d) => ({ id: d.id, ...d.data() }) as RecentReview));

      let pendingReportsCount = 0;
      try {
        const [prSnap, rrSnap, phSnap] = await Promise.all([
          getDocs(query(collection(db, 'place_reports'), where('status', '==', 'pending'), orderBy('createdAtMillis', 'desc'))),
          getDocs(query(collection(db, 'review_reports'), where('status', '==', 'pending'), orderBy('createdAtMillis', 'desc'))),
          getDocs(query(collection(db, 'photo_reports'), where('status', '==', 'pending'), orderBy('createdAtMillis', 'desc'))),
        ]);

        const reports: RecentReport[] = [
          ...prSnap.docs.map((d) => ({ id: d.id, type: 'place' as const, reason: d.data().reason || '', comment: d.data().comment || '', createdAtMillis: d.data().createdAtMillis || 0 })),
          ...rrSnap.docs.map((d) => ({ id: d.id, type: 'review' as const, reason: d.data().reason || '', comment: d.data().comment || '', createdAtMillis: d.data().createdAtMillis || 0 })),
          ...phSnap.docs.map((d) => ({ id: d.id, type: 'photo' as const, reason: d.data().reason || '', comment: d.data().comment || '', createdAtMillis: d.data().createdAtMillis || 0 })),
        ].sort((a, b) => b.createdAtMillis - a.createdAtMillis);

        pendingReportsCount = reports.length;
        const clearedAt = getDashboardReportsClearedAt();
        const unreadReports = reports.filter((report) => report.createdAtMillis > clearedAt);
        setNewReportsCount(unreadReports.length);
        setRecentReports(unreadReports.slice(0, 5));
      } catch (reportErr) {
        console.error('Failed to fetch reports for dashboard:', reportErr);
        setRecentReports([]);
        setNewReportsCount(0);
      }

      setStats({
        places: placesC.data().count,
        reviews: reviewsC.data().count,
        users: usersC.data().count,
        pendingReports: pendingReportsCount,
      });
    } catch (err) {
      console.error('Dashboard fetch error:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void fetchDashboard();
    const refresh = () => void fetchDashboard();
    window.addEventListener(REPORTS_CLEARED_EVENT, refresh);
    return () => window.removeEventListener(REPORTS_CLEARED_EVENT, refresh);
  }, [fetchDashboard]);

  function handleClearNewReports() {
    clearDashboardReports();
    setRecentReports([]);
    setNewReportsCount(0);
  }

  if (loading) {
    return <Box display="flex" justifyContent="center" py={6}><CircularProgress /></Box>;
  }

  const statCards = [
    { label: 'Miejsca', value: stats.places, icon: <PlaceIcon sx={{ fontSize: 36, color: '#1976D2' }} />, border: '#1976D2' },
    { label: 'Opinie', value: stats.reviews, icon: <ReviewsIcon sx={{ fontSize: 36, color: '#388E3C' }} />, border: '#388E3C' },
    { label: 'Użytkownicy', value: stats.users, icon: <PeopleIcon sx={{ fontSize: 36, color: '#7B1FA2' }} />, border: '#7B1FA2' },
    { label: 'Zgłoszenia oczekujące', value: stats.pendingReports, icon: <ReportIcon sx={{ fontSize: 36, color: '#D32F2F' }} />, border: '#D32F2F' },
  ];

  return (
    <Box>
      <Typography variant="h4" fontWeight={700} mb={1}>Dashboard</Typography>
      <Typography variant="body2" color="text.secondary" mb={3}>Podsumowanie statystyk i ostatnia aktywność w kidZone.</Typography>

      <Grid container spacing={3} mb={4}>
        {statCards.map((card) => (
          <Grid item xs={12} sm={6} md={3} key={card.label}>
            <Card sx={{ borderLeft: `4px solid ${card.border}`, height: '100%' }}>
              <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2, height: '100%' }}>
                {card.icon}
                <Box>
                  <Typography variant="h5" fontWeight={700}>{card.value}</Typography>
                  <Typography variant="body2" color="text.secondary">{card.label}</Typography>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Grid container spacing={3}>
        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 2, overflow: 'hidden' }}>
            <Box display="flex" alignItems="center" gap={1} mb={2}><PersonAddIcon color="primary" /><Typography variant="h6" fontWeight={600}>Nowi użytkownicy</Typography></Box>
            <List dense disablePadding>
              {recentUsers.map((u) => <ListItem key={u.id} disableGutters><ListItemAvatar><Avatar src={u.avatarUrl} sx={{ width: 32, height: 32, fontSize: 14 }}>{u.name?.charAt(0) || '?'}</Avatar></ListItemAvatar><ListItemText primary={u.name || u.email} secondary={formatDate(u.createdAtMillis)} /></ListItem>)}
              {recentUsers.length === 0 && <Typography variant="body2" color="text.secondary">Brak</Typography>}
            </List>
          </Paper>
        </Grid>

        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 2 }}>
            <Box display="flex" alignItems="center" gap={1} mb={2}><NewReleasesIcon color="primary" /><Typography variant="h6" fontWeight={600}>Nowe miejsca</Typography></Box>
            <List dense disablePadding>
              {recentPlaces.map((p) => <ListItem key={p.id} disableGutters><ListItemAvatar><Avatar sx={{ width: 32, height: 32, bgcolor: '#e3f2fd' }}><PlaceIcon sx={{ fontSize: 18, color: '#1976D2' }} /></Avatar></ListItemAvatar><ListItemText primary={p.name} secondary={formatDate(p.createdAtMillis)} />{p.averageRating > 0 && <Chip label={p.averageRating.toFixed(1)} size="small" />}</ListItem>)}
              {recentPlaces.length === 0 && <Typography variant="body2" color="text.secondary">Brak</Typography>}
            </List>
          </Paper>
        </Grid>

        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 2, overflow: 'hidden' }}>
            <Box display="flex" alignItems="center" gap={1} mb={2}><ReviewsIcon color="primary" /><Typography variant="h6" fontWeight={600}>Nowe opinie</Typography></Box>
            <List dense disablePadding>
              {recentReviews.map((r) => <ListItem key={r.id} disableGutters><ListItemText primary={<Box display="flex" alignItems="center" gap={1}><Typography variant="body2" fontWeight={500}>{r.authorName || 'Anonim'}</Typography><Rating value={r.rating} size="small" readOnly /></Box>} secondary={r.comment || '(bez komentarza)'} /></ListItem>)}
              {recentReviews.length === 0 && <Typography variant="body2" color="text.secondary">Brak</Typography>}
            </List>
          </Paper>
        </Grid>
      </Grid>

      <Paper sx={{ p: 2, mt: 3 }}>
        <Box display="flex" alignItems="center" gap={1} mb={2}>
          <ReportIcon color="error" />
          <Typography variant="h6" fontWeight={600}>Nowe zgłoszenia</Typography>
          <Chip label={newReportsCount} size="small" color="error" />
          <Box sx={{ flexGrow: 1 }} />
          <Button size="small" onClick={handleClearNewReports} disabled={newReportsCount === 0}>Wyczyść</Button>
        </Box>
        {recentReports.length === 0 ? (
          <Typography variant="body2" color="text.secondary">Brak nowych zgłoszeń</Typography>
        ) : (
          <List dense disablePadding>
            {recentReports.map((r) => (
              <ListItemButton key={`${r.type}-${r.id}`} onClick={() => navigate(`/reports?tab=${r.type === 'place' ? 0 : r.type === 'review' ? 1 : 2}`)} sx={{ borderRadius: 1, mb: 0.5 }}>
                <ListItemAvatar>
                  <Avatar sx={{ width: 32, height: 32, bgcolor: r.type === 'place' ? '#ffebee' : r.type === 'review' ? '#fff3e0' : '#fce4ec' }}>
                    {r.type === 'place' && <PlaceIcon sx={{ fontSize: 18, color: '#D32F2F' }} />}
                    {r.type === 'review' && <WarningIcon sx={{ fontSize: 18, color: '#F57C00' }} />}
                    {r.type === 'photo' && <PhotoIcon sx={{ fontSize: 18, color: '#C2185B' }} />}
                  </Avatar>
                </ListItemAvatar>
                <ListItemText primary={<Box display="flex" alignItems="center" gap={1}><Chip label={r.type === 'place' ? 'Miejsce' : r.type === 'review' ? 'Opinia' : 'Zdjęcie'} size="small" variant="outlined" /><Typography variant="body2">{REASON_LABELS[r.reason] || r.reason}</Typography></Box>} secondary={`${formatDate(r.createdAtMillis)}${r.comment ? ` — ${r.comment}` : ''}`} />
              </ListItemButton>
            ))}
          </List>
        )}
      </Paper>
    </Box>
  );
}
