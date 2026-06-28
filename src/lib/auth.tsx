// Auth context: holds the current user, exposes login/register/logout, and keeps
// the JWT in sync between memory (for API requests) and localStorage (so the
// session survives a reload).
//
// SSR-safe: the initial render is always logged-out (user = null) on both server
// and client, and localStorage is only touched inside effects/handlers — never
// during render — so hydration stays clean.

import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";

import {
  type AuthUser,
  fetchMe,
  loginUser,
  registerUser,
  setAuthToken,
} from "@/lib/api";

const TOKEN_KEY = "algorythm_token";

interface AuthContextValue {
  user: AuthUser | null;
  /** True while we are checking a stored token on first load. */
  loading: boolean;
  login: (username: string, password: string) => Promise<void>;
  register: (username: string, email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);

  // On mount (client only): if we have a stored token, validate it via /me.
  useEffect(() => {
    const token = localStorage.getItem(TOKEN_KEY);
    if (!token) {
      setLoading(false);
      return;
    }
    setAuthToken(token);
    fetchMe()
      .then((u) => setUser(u))
      .catch(() => {
        // stale/invalid token — drop it
        localStorage.removeItem(TOKEN_KEY);
        setAuthToken(null);
      })
      .finally(() => setLoading(false));
  }, []);

  const persist = (token: string, u: AuthUser) => {
    localStorage.setItem(TOKEN_KEY, token);
    setAuthToken(token);
    setUser(u);
  };

  const login = async (username: string, password: string) => {
    const res = await loginUser({ username, password });
    persist(res.token, res.user);
  };

  const register = async (username: string, email: string, password: string) => {
    await registerUser({ username, email, password });
    // log in right after registering so the user is signed in immediately
    const res = await loginUser({ username, password });
    persist(res.token, res.user);
  };

  const logout = () => {
    localStorage.removeItem(TOKEN_KEY);
    setAuthToken(null);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within an AuthProvider");
  return ctx;
}
