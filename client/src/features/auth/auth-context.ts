// The auth context object and the useAuth hook. Kept in a plain .ts file
// (no component) so the provider .tsx can stay fast-refresh friendly.

import { createContext, useContext } from "react";
import type { AuthUser } from "@/lib/auth";
import type { LoginResponseDTO } from "@/types/auth";

export interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  login: (response: LoginResponseDTO) => void;
  logout: () => void;
}

export const AuthContext = createContext<AuthContextValue | null>(null);

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (context === null) {
    throw new Error("useAuth must be used inside <AuthProvider>");
  }
  return context;
}
