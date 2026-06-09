import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Grid,
  Card,
  CardContent,
  Typography,
  CircularProgress,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  ListItemButton,
  Avatar,
  Chip,
  Paper,
  Rating,
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
  query,
  orderBy,
  getDocs,
  getCountFromServer,
  where,
  limit,
} from 'firebase/firestore';
import { db } from '../services/firebase';

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
  category: string;
  createdAtMillis: number;
  averageRating: number;
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

  useEffect(() => {
    fetchDashboard();
  }, []);

  async function fetchDashboard() {
    try {
      // Stats
      const [placesC, reviewsC, usersC, prC, rrC, phC] = await Promise.all([
        getCountFromServer(collection(db, 'places')),
        getCountFromServer(collection(db, 'reviews')),
        getCountFromServer(collection(db, 'users')),
        getCountFromServer(query(collection(db, 'place_reports'), where('status', '==', 'pending'))),
        getCountFromServer(query(collection(db, 'review_reports'), where('status', '==', 'pending'))),
        getCountFromServer(query(collection(db, 'photo_reports'), where('status', '==', 'pending'))),
      ]);
      setStats({
        places: placesC.data().count,
        reviews: reviewsC.data().count,
        users: usersC.data().count,
        pendingReports: prC.data().count + rrC.data().count + phC.data().count,
      });

      // Recent users
      const usersSnap = await getDocs(query(collection(db, 'users'), orderBy('createdAtMillis', 'desc'), limit(5)));
      setRecentUsers(usersSnap.docs.map((d) => ({ id: d.id, ...d.data() } as RecentUser)));

      // Recent places
      const placesSnap = await getDocs(query(collection(db, 'places'), orderBy('createdAtMillis', 'desc'), limit(5)));
      setRecentPlaces(placesSnap.docs.map((d) => ({ id: d.id, ...d.data() } as RecentPlace)));

      // Recent reviews
      const reviewsSnap = await getDocs(query(collection(db, 'reviews'), orderBy('createdAtMillis', 'desc'), limit(5)));
      setRecentReviews(reviewsSnap.docs.map((d) => ({ id: d.id, ...d.data() } as RecentReview)));

      // Recent reports (pending) - no orderBy to avoid composite index requirement
      const [prSnap2, rrSnap2, phSnap2] = await Promise.all([
        getDocs(query(collection(db, 'place_reports'), where('status', '==', 'pending'))),
        getDocs(query(collection(db, 'review_reports'), where('status', '==', 'pending'))),
        getDocs(query(collection(db, 'photo_reports'), where('status', '==', 'pending'))),
      ]);
      const reports: RecentReport[] = [
        ...prSnap2.docs.map((d) => ({ id: d.id, type: 'place' as const, reason: d.data().reason || '', comment: d.data().comment || '', createdAtMillis: d.data().createdAtMillis || 0 })),
        ...rrSnap2.docs.map((d) => ({ id: d.id, type: 'review' as const, reason: d.data().reason || '', comment: d.data().comment || '', createdAtMillis: d.data().createdAtMillis || 0 })),
        ...phSnap2.docs.map((d) => ({ id: d.id, type: 'photo' as const, reason: d.data().reason || '', comment: d.data().comment || '', createdAtMillis: d.data().createdAtMillis || 0 })),
      ];
      reports.sort((a, b) => b.createdAtMillis - a.createdAtMillis);
      setRecentReports(reports.slice(0, 5));
    } catch (err) {
      console.error('Dashboard fetch error:', err);
    } finally {
      setLoading(false);
    }
  }

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" py={6}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="h4" fontWeight={700} mb={1}>
        Dashboard
      </Typography>
      <Typography variant="body2" color="text.secondary" mb={3}>
        Podsumowanie statystyk i ostatnia aktywność w kidZone.
      </Typography>

      {/* Stats cards */}
      <Grid container spacing={3} mb={4}>
        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ borderLeft: '4px solid #1976D2', height: '100%' }}>
            <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2, height: '100%' }}>
              <PlaceIcon sx={{ fontSize: 36, color: '#1976D2' }} />
              <Box>
                <Typography variant="h5" fontWeight={700}>{stats.places}</Typography>
                <Typography variant="body2" color="text.secondary">Miejsca</Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ borderLeft: '4px solid #388E3C', height: '100%' }}>
            <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2, height: '100%' }}>
              <ReviewsIcon sx={{ fontSize: 36, color: '#388E3C' }} />
              <Box>
                <Typography variant="h5" fontWeight={700}>{stats.reviews}</Typography>
                <Typography variant="body2" color="text.secondary">Opinie</Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ borderLeft: '4px solid #7B1FA2', height: '100%' }}>
            <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2, height: '100%' }}>
              <PeopleIcon sx={{ fontSize: 36, color: '#7B1FA2' }} />
              <Box>
                <Typography variant="h5" fontWeight={700}>{stats.users}</Typography>
                <Typography variant="body2" color="text.secondary">Użytkownicy</Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ borderLeft: '4px solid #D32F2F', height: '100%' }}>
            <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2, height: '100%' }}>
              <ReportIcon sx={{ fontSize: 36, color: '#D32F2F' }} />
              <Box>
                <Typography variant="h5" fontWeight={700}>{stats.pendingReports}</Typography>
                <Typography variant="body2" color="text.secondary">Zgłoszenia oczekujące</Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Recent activity */}
      <Grid container spacing={3}>
        {/* Recent users */}
        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 2 }}>
            <Box display="flex" alignItems="center" gap={1} mb={2}>
              <PersonAddIcon color="primary" />
              <Typography variant="h6" fontWeight={600}>Nowi użytkownicy</Typography>
            </Box>
            <List dense disablePadding>
              {recentUsers.map((u) => (
                <ListItem key={u.id} disableGutters>
                  <ListItemAvatar>
                    <Avatar src={u.avatarUrl} sx={{ width: 32, height: 32, fontSize: 14 }}>
                      {u.name?.charAt(0) || '?'}
                    </Avatar>
                  </ListItemAvatar>
                  <ListItemText
                    primary={u.name || u.email}
                    secondary={formatDate(u.createdAtMillis)}
                    primaryTypographyProps={{ variant: 'body2', fontWeight: 500 }}
                    secondaryTypographyProps={{ variant: 'caption' }}
                  />
                </ListItem>
              ))}
              {recentUsers.length === 0 && (
                <Typography variant="body2" color="text.secondary">Brak</Typography>
              )}
            </List>
          </Paper>
        </Grid>

        {/* Recent places */}
        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 2 }}>
            <Box display="flex" alignItems="center" gap={1} mb={2}>
              <NewReleasesIcon color="primary" />
              <Typography variant="h6" fontWeight={600}>Nowe miejsca</Typography>
            </Box>
            <List dense disablePadding>
              {recentPlaces.map((p) => (
                <ListItem key={p.id} disableGutters>
                  <ListItemAvatar>
                    <Avatar sx={{ width: 32, height: 32, bgcolor: '#e3f2fd' }}>
                      <PlaceIcon sx={{ fontSize: 18, color: '#1976D2' }} />
                    </Avatar>
                  </ListItemAvatar>
                  <ListItemText
                    primary={p.name}
                    secondary={formatDate(p.createdAtMillis)}
                    primaryTypographyProps={{ variant: 'body2', fontWeight: 500 }}
                    secondaryTypographyProps={{ variant: 'caption' }}
                  />
                  {p.averageRating > 0 && (
                    <Chip label={p.averageRating.toFixed(1)} size="small" />
                  )}
                </ListItem>
              ))}
              {recentPlaces.length === 0 && (
                <Typography variant="body2" color="text.secondary">Brak</Typography>
              )}
            </List>
          </Paper>
        </Grid>

        {/* Recent reviews */}
        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 2 }}>
            <Box display="flex" alignItems="center" gap={1} mb={2}>
              <ReviewsIcon color="primary" />
              <Typography variant="h6" fontWeight={600}>Nowe opinie</Typography>
            </Box>
            <List dense disablePadding>
              {recentReviews.map((r) => (
                <ListItem key={r.id} disableGutters sx={{ alignItems: 'flex-start' }}>
                  <ListItemText
                    primary={
                      <Box display="flex" alignItems="center" gap={1}>
                        <Typography variant="body2" fontWeight={500}>{r.authorName || 'Anonim'}</Typography>
                        <Rating value={r.rating} size="small" readOnly />
                      </Box>
                    }
                    secondary={r.comment ? (r.comment.length > 60 ? r.comment.slice(0, 60) + '...' : r.comment) : '(bez komentarza)'}
                    secondaryTypographyProps={{ variant: 'caption' }}
                  />
                </ListItem>
              ))}
              {recentReviews.length === 0 && (
                <Typography variant="body2" color="text.secondary">Brak</Typography>
              )}
            </List>
          </Paper>
        </Grid>
      </Grid>

      {/* Recent reports */}
      <Paper sx={{ p: 2, mt: 3 }}>
        <Box display="flex" alignItems="center" gap={1} mb={2}>
          <ReportIcon color="error" />
          <Typography variant="h6" fontWeight={600}>Nowe zgłoszenia</Typography>
          <Chip label={recentReports.length} size="small" color="error" />
        </Box>
        {recentReports.length === 0 ? (
          <Typography variant="body2" color="text.secondary">Brak oczekujących zgłoszeń</Typography>
        ) : (
          <List dense disablePadding>
            {recentReports.map((r) => (
              <ListItemButton
                key={r.id}
                onClick={() => {
                  if (r.type === 'place') navigate('/reports?tab=0');
                  else if (r.type === 'review') navigate('/reports?tab=1');
                  else navigate('/reports?tab=2');
                }}
                sx={{ borderRadius: 1, mb: 0.5 }}
              >
                <ListItemAvatar>
                  <Avatar sx={{ width: 32, height: 32, bgcolor: r.type === 'place' ? '#ffebee' : r.type === 'review' ? '#fff3e0' : '#fce4ec' }}>
                    {r.type === 'place' && <PlaceIcon sx={{ fontSize: 18, color: '#D32F2F' }} />}
                    {r.type === 'review' && <WarningIcon sx={{ fontSize: 18, color: '#F57C00' }} />}
                    {r.type === 'photo' && <PhotoIcon sx={{ fontSize: 18, color: '#C2185B' }} />}
                  </Avatar>
                </ListItemAvatar>
                <ListItemText
                  primary={
                    <Box display="flex" alignItems="center" gap={1}>
                      <Chip
                        label={r.type === 'place' ? 'Miejsce' : r.type === 'review' ? 'Opinia' : 'Zdjęcie'}
                        size="small"
                        variant="outlined"
                        color={r.type === 'place' ? 'error' : r.type === 'review' ? 'warning' : 'secondary'}
                      />
                      <Typography variant="body2">{r.reason}</Typography>
                    </Box>
                  }
                  secondary={`${formatDate(r.createdAtMillis)}${r.comment ? ' — ' + (r.comment.length > 40 ? r.comment.slice(0, 40) + '...' : r.comment) : ''}`}
                  secondaryTypographyProps={{ variant: 'caption' }}
                />
              </ListItemButton>
            ))}
          </List>
        )}
      </Paper>
    </Box>
  );
}
