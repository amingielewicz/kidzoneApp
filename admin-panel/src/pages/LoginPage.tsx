import { useState, FormEvent } from 'react';
import {
  Box,
  Card,
  CardContent,
  TextField,
  Button,
  Typography,
  Alert,
  CircularProgress,
  Divider,
} from '@mui/material';
import GoogleIcon from '@mui/icons-material/Google';
import { useAuth } from '../hooks/useAuth';

export function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const { signIn, signInWithGoogle, loading, error, user, isAdmin } = useAuth();

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    await signIn(email, password);
  };

  const showAccessDenied = user && !isAdmin;

  return (
    <Box
      component="main"
      aria-labelledby="admin-login-title"
      display="flex"
      justifyContent="center"
      alignItems="center"
      minHeight="100vh"
      bgcolor="background.default"
    >
      <Card sx={{ maxWidth: 400, width: '100%', mx: 2 }}>
        <CardContent sx={{ p: 4 }}>
          <Box display="flex" flexDirection="column" alignItems="center" mb={3}>
            <Box
              component="img"
              src="/logo.png"
              alt="kidZone"
              sx={{ width: 64, height: 64, mb: 2 }}
            />
            <Typography id="admin-login-title" variant="h5" fontWeight={700}>
              kidZone Admin
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Panel administracyjny
            </Typography>
          </Box>

          {error && (
            <Alert id="login-error" role="alert" severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}

          {showAccessDenied && (
            <Alert id="login-access-denied" role="alert" severity="warning" sx={{ mb: 2 }}>
              To konto nie ma uprawnień administratora.
            </Alert>
          )}

          {/* Google Sign-In */}
          <Button
            fullWidth
            variant="outlined"
            size="large"
            startIcon={<GoogleIcon />}
            disabled={loading}
            aria-busy={loading}
            aria-describedby={
              error ? 'login-error' : showAccessDenied ? 'login-access-denied' : undefined
            }
            onClick={signInWithGoogle}
            sx={{
              mb: 2,
              borderColor: '#dadce0',
              color: '#3c4043',
              textTransform: 'none',
              fontWeight: 500,
              '&:hover': {
                borderColor: '#d2e3fc',
                bgcolor: '#f8faff',
              },
            }}
          >
            {loading ? (
              <>
                <CircularProgress size={20} aria-hidden="true" sx={{ mr: 1 }} />
                Logowanie...
              </>
            ) : (
              'Zaloguj się przez Google'
            )}
          </Button>

          <Divider sx={{ my: 2 }}>
            <Typography variant="caption" color="text.secondary">
              lub email i hasło
            </Typography>
          </Divider>

          <form
            onSubmit={handleSubmit}
            aria-describedby={
              error ? 'login-error' : showAccessDenied ? 'login-access-denied' : undefined
            }
          >
            <TextField
              fullWidth
              label="Email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              margin="normal"
              required
            />
            <TextField
              fullWidth
              label="Hasło"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              margin="normal"
              required
            />
            <Button
              type="submit"
              fullWidth
              variant="contained"
              size="large"
              disabled={loading}
              aria-busy={loading}
              sx={{ mt: 2 }}
            >
              {loading ? (
                <>
                  <CircularProgress size={20} color="inherit" aria-hidden="true" sx={{ mr: 1 }} />
                  Logowanie...
                </>
              ) : (
                'Zaloguj się'
              )}
            </Button>
          </form>
        </CardContent>
      </Card>
    </Box>
  );
}
