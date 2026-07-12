import { ReactNode, useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  AppBar,
  Badge,
  Box,
  Button,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import DashboardIcon from '@mui/icons-material/Dashboard';
import ReportIcon from '@mui/icons-material/Report';
import EditNoteIcon from '@mui/icons-material/EditNote';
import PlaceIcon from '@mui/icons-material/Place';
import PeopleIcon from '@mui/icons-material/People';
import LogoutIcon from '@mui/icons-material/Logout';
import { collection, getDocs, orderBy, query, where } from 'firebase/firestore';
import { useAuth } from '../hooks/useAuth';
import { db } from '../services/firebase';
import {
  getDashboardReportsClearedAt,
  REPORTS_CLEARED_EVENT,
} from '../services/reportNotifications';

const DRAWER_WIDTH = 240;

const communityItems = [
  { path: '/', label: 'Dashboard', icon: <DashboardIcon /> },
  { path: '/reports', label: 'Zgłoszenia', icon: <ReportIcon /> },
  { path: '/change-requests', label: 'Propozycje zmian', icon: <EditNoteIcon /> },
];

const administrationItems = [
  { path: '/places', label: 'Miejsca', icon: <PlaceIcon /> },
  { path: '/users', label: 'Użytkownicy', icon: <PeopleIcon /> },
];

interface LayoutProps {
  children: ReactNode;
}

export function Layout({ children }: LayoutProps) {
  const [mobileOpen, setMobileOpen] = useState(false);
  const [newReportsCount, setNewReportsCount] = useState(0);
  const navigate = useNavigate();
  const location = useLocation();
  const { signOut, user } = useAuth();

  useEffect(() => {
    let active = true;

    async function fetchNewReportsCount() {
      try {
        const reportQueries = ['place_reports', 'review_reports', 'photo_reports'].map((name) =>
          query(collection(db, name), where('status', '==', 'pending'), orderBy('createdAtMillis', 'desc')),
        );
        const snapshots = await Promise.all(reportQueries.map((reportQuery) => getDocs(reportQuery)));
        const clearedAt = getDashboardReportsClearedAt();
        const count = snapshots.reduce(
          (sum, snapshot) =>
            sum + snapshot.docs.filter((document) => (document.data().createdAtMillis || 0) > clearedAt).length,
          0,
        );
        if (active) setNewReportsCount(count);
      } catch (error) {
        console.error('Failed to fetch header report count:', error);
        if (active) setNewReportsCount(0);
      }
    }

    void fetchNewReportsCount();
    window.addEventListener(REPORTS_CLEARED_EVENT, fetchNewReportsCount);
    const intervalId = window.setInterval(fetchNewReportsCount, 60_000);

    return () => {
      active = false;
      window.removeEventListener(REPORTS_CLEARED_EVENT, fetchNewReportsCount);
      window.clearInterval(intervalId);
    };
  }, [location.pathname]);

  useEffect(() => {
    function handleConfirmationKeys(event: KeyboardEvent) {
      const dialog = document.querySelector<HTMLElement>('[role="dialog"]');
      if (!dialog) return;

      if (event.key === 'Escape') {
        return;
      }

      if (event.key !== 'Enter') return;
      const target = event.target as HTMLElement | null;
      if (target?.matches('textarea, input, [contenteditable="true"]')) return;

      const buttons = Array.from(dialog.querySelectorAll<HTMLButtonElement>('button:not(:disabled)'));
      const confirmButton = buttons.find((button) =>
        /^(potwierdź|zatwierdź|usuń|odrzuć)$/i.test(button.textContent?.trim() || ''),
      );
      if (confirmButton) {
        event.preventDefault();
        confirmButton.click();
      }
    }

    document.addEventListener('keydown', handleConfirmationKeys);
    return () => document.removeEventListener('keydown', handleConfirmationKeys);
  }, []);

  function renderMenuItems(items: typeof communityItems) {
    return items.map((item) => (
      <ListItemButton
        key={item.path}
        selected={location.pathname === item.path}
        aria-current={location.pathname === item.path ? 'page' : undefined}
        onClick={() => {
          navigate(item.path);
          setMobileOpen(false);
        }}
      >
        <ListItemIcon>{item.icon}</ListItemIcon>
        <ListItemText primary={item.label} />
      </ListItemButton>
    ));
  }

  const drawer = (
    <Box component="nav" aria-label="Nawigacja panelu administracyjnego">
      <Toolbar>
        <Typography variant="h6" fontWeight={700} color="primary">kidZone Admin</Typography>
      </Toolbar>
      <Divider />
      <List aria-label="Zgłoszenia społeczności">
        {renderMenuItems(communityItems)}
      </List>
      <Divider sx={{ mx: 2 }} />
      <List aria-label="Zarządzanie administracyjne">
        {renderMenuItems(administrationItems)}
      </List>
      <Divider />
      <Box px={2} py={1}>
        <Typography variant="caption" color="text.secondary">{user?.email}</Typography>
      </Box>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex' }}>
      <AppBar position="fixed" sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}>
        <Toolbar>
          <IconButton
            color="inherit"
            edge="start"
            aria-label={mobileOpen ? 'Zamknij menu nawigacji' : 'Otwórz menu nawigacji'}
            aria-controls="admin-mobile-navigation"
            aria-expanded={mobileOpen}
            onClick={() => setMobileOpen(!mobileOpen)}
            sx={{ mr: 2, display: { md: 'none' } }}
          >
            <MenuIcon />
          </IconButton>
          <Box component="img" src="/logo.png" alt="kidZone" sx={{ height: 32, mr: 1 }} />
          <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1 }}>
            kidZone Admin Panel
          </Typography>
          <IconButton color="inherit" aria-label={`Nowe zgłoszenia: ${newReportsCount}`} onClick={() => navigate('/')} sx={{ mr: 1 }}>
            <Badge badgeContent={newReportsCount} color="error" max={99}>
              <ReportIcon />
            </Badge>
          </IconButton>
          <Button color="inherit" startIcon={<LogoutIcon />} onClick={signOut}>Wyloguj</Button>
        </Toolbar>
      </AppBar>

      <Drawer
        id="admin-mobile-navigation"
        variant="temporary"
        open={mobileOpen}
        onClose={() => setMobileOpen(false)}
        ModalProps={{ keepMounted: true }}
        PaperProps={{ 'aria-label': 'Mobilna nawigacja panelu administracyjnego' }}
        sx={{ display: { xs: 'block', md: 'none' }, '& .MuiDrawer-paper': { width: DRAWER_WIDTH } }}
      >
        {drawer}
      </Drawer>

      <Drawer
        variant="permanent"
        PaperProps={{ 'aria-label': 'Nawigacja panelu administracyjnego' }}
        sx={{ display: { xs: 'none', md: 'block' }, '& .MuiDrawer-paper': { width: DRAWER_WIDTH, boxSizing: 'border-box' } }}
        open
      >
        {drawer}
      </Drawer>

      <Box
        component="main"
        aria-label="Główna treść panelu administracyjnego"
        sx={{ flexGrow: 1, p: 3, width: { md: `calc(100% - ${DRAWER_WIDTH}px)` }, ml: { md: `${DRAWER_WIDTH}px` }, mt: '64px', minHeight: 'calc(100vh - 64px)' }}
      >
        {children}
      </Box>
    </Box>
  );
}
