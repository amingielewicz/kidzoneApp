import { useEffect, useState } from 'react';
import {
  Box,
  Grid,
  Card,
  CardContent,
  Typography,
  CircularProgress,
} from '@mui/material';
import PlaceIcon from '@mui/icons-material/Place';
import ReviewsIcon from '@mui/icons-material/RateReview';
import PeopleIcon from '@mui/icons-material/People';
import ReportIcon from '@mui/icons-material/Report';
import WarningIcon from '@mui/icons-material/Warning';
import PhotoIcon from '@mui/icons-material/Photo';
import { collection, getCountFromServer, query, where } from 'firebase/firestore';
import { db } from '../services/firebase';

interface StatCard {
  label: string;
  value: number | null;
  icon: React.ReactNode;
  color: string;
}

export function DashboardPage() {
  const [stats, setStats] = useState<StatCard[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchStats() {
      try {
        const [
          placesSnap,
          reviewsSnap,
          usersSnap,
          placeReportsSnap,
          reviewReportsSnap,
          photoReportsSnap,
        ] = await Promise.all([
          getCountFromServer(collection(db, 'places')),
          getCountFromServer(collection(db, 'reviews')),
          getCountFromServer(collection(db, 'users')),
          getCountFromServer(
            query(collection(db, 'place_reports'), where('status', '==', 'pending'))
          ),
          getCountFromServer(
            query(collection(db, 'review_reports'), where('status', '==', 'pending'))
          ),
          getCountFromServer(
            query(collection(db, 'photo_reports'), where('status', '==', 'pending'))
          ),
        ]);

        setStats([
          {
            label: 'Miejsca',
            value: placesSnap.data().count,
            icon: <PlaceIcon sx={{ fontSize: 40 }} />,
            color: '#1976D2',
          },
          {
            label: 'Opinie',
            value: reviewsSnap.data().count,
            icon: <ReviewsIcon sx={{ fontSize: 40 }} />,
            color: '#388E3C',
          },
          {
            label: 'Użytkownicy',
            value: usersSnap.data().count,
            icon: <PeopleIcon sx={{ fontSize: 40 }} />,
            color: '#7B1FA2',
          },
          {
            label: 'Zgłoszenia miejsc',
            value: placeReportsSnap.data().count,
            icon: <ReportIcon sx={{ fontSize: 40 }} />,
            color: '#D32F2F',
          },
          {
            label: 'Zgłoszenia opinii',
            value: reviewReportsSnap.data().count,
            icon: <WarningIcon sx={{ fontSize: 40 }} />,
            color: '#F57C00',
          },
          {
            label: 'Zgłoszenia zdjęć',
            value: photoReportsSnap.data().count,
            icon: <PhotoIcon sx={{ fontSize: 40 }} />,
            color: '#C2185B',
          },
        ]);
      } catch (err) {
        console.error('Failed to fetch stats:', err);
      } finally {
        setLoading(false);
      }
    }

    fetchStats();
  }, []);

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" py={6}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="h4" fontWeight={700} mb={3}>
        Dashboard
      </Typography>

      <Grid container spacing={3}>
        {stats.map((stat) => (
          <Grid key={stat.label} size={{ xs: 12, sm: 6, md: 4 }}>
            <Card
              sx={{
                height: '100%',
                borderLeft: `4px solid ${stat.color}`,
              }}
            >
              <CardContent
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 2,
                }}
              >
                <Box sx={{ color: stat.color }}>{stat.icon}</Box>
                <Box>
                  <Typography variant="h4" fontWeight={700}>
                    {stat.value ?? '—'}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {stat.label}
                  </Typography>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Box>
  );
}
