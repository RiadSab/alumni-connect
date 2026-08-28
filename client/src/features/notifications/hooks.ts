// React Query hooks for notifications.

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { notificationsApi } from "@/api/notifications";
import { queryKeys } from "@/lib/queryKeys";
import { useAuth } from "@/features/auth/auth-context";
import type { NotificationFilters } from "@/types/notification";

export function useNotifications(params?: NotificationFilters) {
  return useQuery({
    queryKey: queryKeys.notifications.list(params),
    queryFn: () => notificationsApi.mine(params),
  });
}

export function useUnreadNotifications() {
  const { isAuthenticated } = useAuth();
  return useQuery({
    queryKey: queryKeys.notifications.unread(),
    queryFn: () => notificationsApi.unread(),
    enabled: isAuthenticated,
  });
}

export function useUnreadNotificationCount() {
  const { isAuthenticated } = useAuth();
  return useQuery({
    queryKey: queryKeys.notifications.unreadCount(),
    queryFn: () => notificationsApi.unreadCount(),
    enabled: isAuthenticated,
  });
}

export function useMarkNotificationRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => notificationsApi.markRead(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.notifications.all() }),
  });
}

export function useMarkAllNotificationsRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => notificationsApi.markAllRead(),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.notifications.all() }),
  });
}
