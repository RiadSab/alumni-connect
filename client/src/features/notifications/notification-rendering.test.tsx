import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";

import { AuthContext } from "@/features/auth/auth-context";
import { LangProvider } from "@/features/i18n/LangProvider";
import { queryKeys } from "@/lib/queryKeys";
import { CandidateDashboardPage } from "@/pages/CandidateDashboardPage";
import { NotificationsPage } from "@/pages/NotificationsPage";
import type { NotificationDTO } from "@/types/notification";

const cases = [
  ["APPLICATION_ACCEPTED", "Developer", "Acme", "You've been accepted for Developer at Acme"],
  ["APPLICATION_REJECTED", "Developer", "Acme", "Your application for Developer at Acme wasn't taken further"],
  ["INTERVIEW_SCHEDULED", "Developer", "Acme", "Interview scheduled for Developer at Acme"],
  ["MENTORING_SLOT_BOOKED", "DSA", "Omar", "Omar booked one of your mock interview slots"],
  ["MENTORING_SESSION_CANCELLED", "DSA", "Friday at 19:00", "Your mock interview on Friday at 19:00 was cancelled"],
  [
    "MENTORING_SESSION_RELEASED",
    "DSA",
    "Friday at 19:00",
    "Your mock interview slot on Friday at 19:00 became available again because you hadn't confirmed",
  ],
  ["FUTURE_EVENT", null, null, "You have an update"],
] as const;

describe.each([
  ["Notifications page", NotificationsPage],
  ["Candidate dashboard", CandidateDashboardPage],
] as const)("%s", (_name, Page) => {
  it.each(cases)("renders %s and preserves its link", (type, subject, context, message) => {
    const client = new QueryClient({
      defaultOptions: { queries: { staleTime: Infinity } },
    });
    const notification: NotificationDTO = {
      id: 1,
      type,
      subject,
      context,
      link: "/mentoring",
      createdAt: "2026-11-18T12:00:00Z",
      readAt: null,
    };
    client.setQueryData(queryKeys.notifications.list({ page: 0 }), {
      content: [notification],
      empty: false,
      totalPages: 1,
    });
    client.setQueryData(queryKeys.notifications.unread(), [notification]);
    client.setQueryData(queryKeys.candidate.me(), null);
    client.setQueryData(queryKeys.jobApplications.myStats(), { total: 0, active: 0, accepted: 0 });
    client.setQueryData(queryKeys.jobApplications.mine({ size: 5 }), { content: [] });
    client.setQueryData(queryKeys.jobOffers.recommended({ size: 5 }), { content: [] });

    render(
      <QueryClientProvider client={client}>
        <AuthContext.Provider
          value={{ user: null, isAuthenticated: true, login: () => {}, logout: () => {} }}
        >
          <MemoryRouter>
            <LangProvider><Page /></LangProvider>
          </MemoryRouter>
        </AuthContext.Provider>
      </QueryClientProvider>,
    );

    expect(screen.getByText(message).closest("a")).toHaveAttribute("href", "/mentoring");
  });
});
