// Minimal JWT token store. Backs the Authorization header injected by http.ts.
// Kept intentionally small for the foundation; session/user state and an auth
// context land in a later phase.

const TOKEN_KEY = "ac.token";

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

export function isAuthenticated(): boolean {
  return getToken() !== null;
}
