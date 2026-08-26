// React Query hooks for the alumni roster (ADMINISTRATOR only).

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { alumniApi } from "@/api/alumni";
import { queryKeys } from "@/lib/queryKeys";
import type { AlumniRecordFilters } from "@/types/alumni";

export function useAlumniRecords(filters?: AlumniRecordFilters) {
  return useQuery({
    queryKey: queryKeys.alumni.records(filters),
    queryFn: () => alumniApi.records(filters),
  });
}

export function useImportRoster() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { file: File; dryRun: boolean }) =>
      alumniApi.importRoster(input.file, input.dryRun),
    // A dry run changes nothing, but refetching after one is harmless and keeps this simple.
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.alumni.all() }),
  });
}
