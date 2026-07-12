import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './hooks/useAuth';
import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPageV2';
import { ReportsPage } from './pages/ReportsPage';
import { ChangeRequestsPage } from './pages/ChangeRequestsPage';
import { PlacesPage } from './pages/PlacesPage';
import { UsersPage } from './pages/UsersPage';
import { Layout } from './components/Layout';
import { Box, CircularProgress, Typography } from '@mui/material';

function App() {
  const { user, isAdmin, loading } = useAuth();

  if (loading) {
    return (
      <Box
        display="flex"
        justifyContent="center"
        alignItems="center"
        minHeight="100vh"
        flexDirection="column"
        gap={2}
      >
        <CircularProgress />
        <Typography color="text.secondary">Wczytywanie...</Typography>
      </Box>
    );
  }

  if (!user || !isAdmin) {
    return (
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    );
  }

  return (
    <Layout>
      <Routes>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/reports" element={<ReportsPage />} />
        <Route path="/change-requests" element={<ChangeRequestsPage />} />
        <Route path="/places" element={<PlacesPage />} />
        <Route path="/users" element={<UsersPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Layout>
  );
}

export default App;
