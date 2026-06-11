import { useState, useEffect } from 'react';
import {
  onAuthStateChanged,
  signInWithEmailAndPassword,
  signInWithPopup,
  GoogleAuthProvider,
  signOut as firebaseSignOut,
  User,
} from 'firebase/auth';
import { doc, getDoc } from 'firebase/firestore';
import { auth, db } from '../services/firebase';

const googleProvider = new GoogleAuthProvider();

interface AuthState {
  user: User | null;
  isAdmin: boolean;
  loading: boolean;
  error: string | null;
}

export function useAuth() {
  const [state, setState] = useState<AuthState>({
    user: null,
    isAdmin: false,
    loading: true,
    error: null,
  });

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (user) => {
      if (user) {
        try {
          // Sprawdź czy user ma rolę admin — najpierw z Custom Claims (token),
          // fallback na Firestore doc jeśli claims jeszcze nie ustawione.
          const tokenResult = await user.getIdTokenResult();
          let isAdmin = tokenResult.claims.admin === true;

          if (!isAdmin) {
            // Fallback: sprawdź pole 'role' w dokumencie Firestore
            const userDoc = await getDoc(doc(db, 'users', user.uid));
            const userData = userDoc.data();
            isAdmin = userData?.role === 'admin';
          }

          setState({ user, isAdmin, loading: false, error: null });
        } catch (err) {
          // Firestore/network niedostępny — wpuść usera ale odmów admina
          console.error('Failed to verify admin status:', err);
          setState({ user, isAdmin: false, loading: false, error: null });
        }
      } else {
        setState({ user: null, isAdmin: false, loading: false, error: null });
      }
    });

    return () => unsubscribe();
  }, []);

  const signIn = async (email: string, password: string) => {
    try {
      setState((prev) => ({ ...prev, loading: true, error: null }));
      await signInWithEmailAndPassword(auth, email, password);
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'Nie udało się zalogować';
      setState((prev) => ({ ...prev, loading: false, error: message }));
    }
  };

  const signInWithGoogle = async () => {
    try {
      setState((prev) => ({ ...prev, loading: true, error: null }));
      await signInWithPopup(auth, googleProvider);
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'Nie udało się zalogować przez Google';
      setState((prev) => ({ ...prev, loading: false, error: message }));
    }
  };

  const signOut = async () => {
    await firebaseSignOut(auth);
  };

  return { ...state, signIn, signInWithGoogle, signOut };
}
