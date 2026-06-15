// Shared TanStack Query client. Imported by main.tsx's QueryClientProvider and
// reused by feature hooks (later phase).

import { QueryClient } from "@tanstack/react-query";
import { isApiError } from "@/lib/http";

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: (failureCount, error) => {
        // Don't retry client errors (401/403/404/409) — only transient failures.
        if (isApiError(error) && error.status < 500) return false;
        return failureCount < 2;
      },
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: false,
    },
  },
});
