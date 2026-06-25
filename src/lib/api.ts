// Base URL for the backend API. Defaults to the backend's published port, which
// works both locally and in Docker (the browser calls localhost:8080 either way).
// Override with VITE_API_URL if the backend lives elsewhere.
export const API_BASE = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

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
