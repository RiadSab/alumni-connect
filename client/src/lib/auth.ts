// Stores the JWT token and the logged-in user in localStorage. The token backs
// the Authorization header in http.ts; the user backs the auth context so the
// app knows who is logged in across page reloads.

import type { UserType } from "@/types/enums";

const TOKEN_KEY = "ac.token";
const USER_KEY = "ac.user";

export interface AuthUser {
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  userType: UserType;
}

// --- Token ---

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

// --- User ---

export function getStoredUser(): AuthUser | null {
  const raw = localStorage.getItem(USER_KEY);
  if (raw === null) return null;
  return JSON.parse(raw) as AuthUser;
}

export function setStoredUser(user: AuthUser): void {
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function clearStoredUser(): void {
  localStorage.removeItem(USER_KEY);
}
