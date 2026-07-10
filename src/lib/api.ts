// Base URL for the backend API. Defaults to the backend's published port, which
// works both locally and in Docker (the browser calls localhost:8080 either way).
// Override with VITE_API_URL if the backend lives elsewhere.
export const API_BASE = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

// The current bearer token, held in memory and attached to every request. The
// auth provider keeps this in sync with localStorage (set on login/hydrate,
// cleared on logout). Kept out of React state so non-React callers can use it too.
let authToken: string | null = null;

export function setAuthToken(token: string | null): void {
  authToken = token;
}

/** Error carrying the HTTP status and the backend's message (for the UI). */
export class ApiError extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

interface RequestOptions {
  method?: string;
  body?: unknown;
}

/** Single fetch helper: JSON in/out, auto Bearer header, typed errors. */
async function apiFetch<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = {};
  if (options.body !== undefined) headers["Content-Type"] = "application/json";
  if (authToken) headers["Authorization"] = `Bearer ${authToken}`;

  const res = await fetch(`${API_BASE}${path}`, {
    method: options.method ?? "GET",
    headers,
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  });

  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    try {
      const data = (await res.json()) as { message?: string; error?: string };
      message = data.message || data.error || message;
    } catch {
      // non-JSON error body — keep the default message
    }
    throw new ApiError(message, res.status);
  }

  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

// --- Health -------------------------------------------------------

/** Pings the backend health endpoint. Returns the body, or null if unreachable. */
export async function getHealth(): Promise<{ status: string } | null> {
  try {
    const res = await fetch(`${API_BASE}/api/health`);
    if (!res.ok) return null;
    return (await res.json()) as { status: string };
  } catch {
    return null;
  }
}

// --- Auth ---------------------------------------------------------

export interface AuthUser {
  id: number;
  username: string;
  email: string;
  createdAt: string;
}

export interface AuthResult {
  token: string;
  tokenType: string;
  user: AuthUser;
}

export function registerUser(body: {
  username: string;
  email: string;
  password: string;
}): Promise<AuthUser> {
  return apiFetch<AuthUser>("/api/auth/register", { method: "POST", body });
}

export function loginUser(body: { username: string; password: string }): Promise<AuthResult> {
  return apiFetch<AuthResult>("/api/auth/login", { method: "POST", body });
}

/** Current user for the active token. Throws ApiError(401) if the token is bad. */
export function fetchMe(): Promise<AuthUser> {
  return apiFetch<AuthUser>("/api/auth/me");
}

// --- Compositions (owner) -----------------------------------------

export interface Composition {
  id: number;
  title: string;
  pattern: string;
  bpm: number;
  isPublic: boolean;
  slug: string | null;
  tags: string[];
  createdAt: string;
  updatedAt: string;
}

export interface CompositionInput {
  title: string;
  pattern: string;
  bpm: number;
  tags: string[];
}
/** All compositions owned by the current user (newest first). */
export function listCompositions(): Promise<Composition[]> {
  return apiFetch<Composition[]>("/api/compositions");
}

export function createComposition(body: CompositionInput): Promise<Composition> {
  return apiFetch<Composition>("/api/compositions", { method: "POST", body });
}

export function updateComposition(id: number, body: CompositionInput): Promise<Composition> {
  return apiFetch<Composition>(`/api/compositions/${id}`, { method: "PUT", body });
}

export function deleteComposition(id: number): Promise<void> {
  return apiFetch<void>(`/api/compositions/${id}`, { method: "DELETE" });
}

export function publishComposition(id: number): Promise<Composition> {
  return apiFetch<Composition>(`/api/compositions/${id}/publish`, { method: "POST" });
}

export function unpublishComposition(id: number): Promise<Composition> {
  return apiFetch<Composition>(`/api/compositions/${id}/unpublish`, { method: "POST" });
}

/**
 * Copies a public composition into the current user's library as a new
 * private composition.
 */
export function remixComposition(slug: string): Promise<Composition> {
  return apiFetch<Composition>(`/api/compositions/remix/${encodeURIComponent(slug)}`, {
    method: "POST",
  });
}

// --- Public compositions + profiles (no auth needed) --------------

export interface PublicComposition {
  slug: string;
  title: string;
  pattern: string;
  bpm: number;
  author: string;
  tags: string[];
  createdAt: string;
  updatedAt: string;
}

/** The explore feed: public compositions, newest first. */
export function listPublicCompositions(
  page = 0,
  size = 20,
  tag?: string,
): Promise<PublicComposition[]> {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });

  if (tag) params.set("tag", tag);

  return apiFetch<PublicComposition[]>(`/api/public/compositions?${params.toString()}`);
}

export function searchPublicCompositions(query: string): Promise<PublicComposition[]> {
  return apiFetch<PublicComposition[]>(
    `/api/public/compositions/search?q=${encodeURIComponent(query)}`,
  );
}

export function getPublicComposition(slug: string): Promise<PublicComposition> {
  return apiFetch<PublicComposition>(`/api/public/compositions/${slug}`);
}

export interface UserProfile {
  username: string;
  joinedAt: string;
  followerCount: number;
  followingCount: number;
  isFollowing: boolean;
  compositions: PublicComposition[];
}

export function getUserProfile(username: string): Promise<UserProfile> {
  return apiFetch<UserProfile>(`/api/public/users/${username}`);
}

// --- Follows + feed (auth) ----------------------------------------

/** Follow a user. No-op on the server if already following. */
export function followUser(username: string): Promise<void> {
  return apiFetch<void>(`/api/users/${username}/follow`, { method: "POST" });
}

/** Stop following a user. No-op on the server if not following. */
export function unfollowUser(username: string): Promise<void> {
  return apiFetch<void>(`/api/users/${username}/follow`, { method: "DELETE" });
}

/** Public compositions by people the current user follows, newest first. */
export function getFollowingFeed(page = 0, size = 20): Promise<PublicComposition[]> {
  return apiFetch<PublicComposition[]>(`/api/feed/following?page=${page}&size=${size}`);
}
// --- Notifications (auth) ------------------------------------------

export type NotificationType = "LIKE" | "COMMENT" | "FOLLOW";

export interface Notification {
  id: number;
  type: NotificationType;
  actor: string;
  compositionId: number | null;
  compositionTitle: string | null;
  compositionSlug: string | null;
  commentId: number | null;
  read: boolean;
  createdAt: string;
}

/** The current user's most recent notifications, newest first. */
export function listNotifications(limit = 20): Promise<Notification[]> {
  return apiFetch<Notification[]>(`/api/notifications?limit=${limit}`);
}

export function getUnreadNotificationCount(): Promise<number> {
  return apiFetch<number>("/api/notifications/unread-count");
}

export function markNotificationRead(id: number): Promise<void> {
  return apiFetch<void>(`/api/notifications/${id}/read`, {
    method: "POST",
  });
}

// --- Account (auth) -----------------------------------------------

export function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  return apiFetch<void>("/api/account/password", {
    method: "PUT",
    body: { currentPassword, newPassword },
  });
}

export function changeEmail(currentPassword: string, email: string): Promise<AuthUser> {
  return apiFetch<AuthUser>("/api/account/email", {
    method: "PUT",
    body: { currentPassword, email },
  });
}

export function deleteAccount(currentPassword: string): Promise<void> {
  return apiFetch<void>("/api/account", {
    method: "DELETE",
    body: { currentPassword },
  });
}
