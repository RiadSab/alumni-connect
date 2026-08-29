// React Query hooks for mentoring.

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { mentoringApi } from "@/api/mentoring";
import { queryKeys } from "@/lib/queryKeys";
import type { PublishSlotInput, SlotFilters } from "@/types/mentoring";

export function useOpenSlots(filters?: SlotFilters) {
  return useQuery({
    queryKey: queryKeys.mentoring.slots(filters),
    queryFn: () => mentoringApi.slots(filters),
  });
}

export function useMySlots() {
  return useQuery({
    queryKey: queryKeys.mentoring.mySlots(),
    queryFn: () => mentoringApi.mySlots(),
  });
}

export function useMyBookings() {
  return useQuery({
    queryKey: queryKeys.mentoring.myBookings(),
    queryFn: () => mentoringApi.myBookings(),
  });
}

export function useRemainingBookings() {
  return useQuery({
    queryKey: queryKeys.mentoring.remaining(),
    queryFn: () => mentoringApi.remaining(),
  });
}

export function usePublishSlot() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: PublishSlotInput) => mentoringApi.publish(body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.mentoring.all() }),
  });
}

export function useCancelSlot() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => mentoringApi.cancelSlot(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.mentoring.all() }),
  });
}

export function useBookSlot() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (slotId: number) => mentoringApi.book(slotId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.mentoring.all() }),
  });
}

export function useCancelBooking() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => mentoringApi.cancelBooking(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.mentoring.all() }),
  });
}
