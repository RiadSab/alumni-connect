import type { ReactNode } from "react";

// Shared shell for the public auth pages: a fixed full-bleed background behind a centered card.
export function AuthScreen({ children }: { children: ReactNode }) {
  return (
    <>
      <div
        aria-hidden
        className="fixed inset-0 -z-10 bg-cover bg-center bg-no-repeat"
        style={{ backgroundImage: "url('/background_login_cmp.png')" }}
      />
      <div className="flex min-h-[calc(100vh-9rem)] items-center justify-center py-4">
        {children}
      </div>
    </>
  );
}
