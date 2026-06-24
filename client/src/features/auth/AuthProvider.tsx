import { useState } from "react";
import type { ReactNode } from "react";
import {
  clearStoredUser,
  clearToken,
  getStoredUser,
  setStoredUser,
  setToken,
} from "@/lib/auth";
import type { AuthUser } from "@/lib/auth";
import type { LoginResponseDTO } from "@/types/auth";
import { queryClient } from "@/lib/queryClient";
import { authApi } from "@/api/auth";
import { AuthContext } from "@/features/auth/auth-context";
import type { AuthContextValue } from "@/features/auth/auth-context";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => getStoredUser());

  function login(response: LoginResponseDTO) {
    const nextUser: AuthUser = {
      userId: response.userId,
      email: response.email,
      firstName: response.firstName,
      lastName: response.lastName,
      userType: response.userType,
    };
    setToken(response.token);
    setStoredUser(nextUser);
    setUser(nextUser);
  }

  function logout() {
    authApi.logout().catch(() => {});
    clearToken();
    clearStoredUser();
    setUser(null);
    // Drop every cached query so the next account doesn't inherit this user's data.
    queryClient.clear();
  }

  const value: AuthContextValue = {
    user,
    isAuthenticated: user !== null,
    login,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
