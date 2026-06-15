// Central query-key factory. Keeping keys in one place keeps cache reads and
// invalidations consistent. Grows as more feature hooks land.

export const queryKeys = {
  candidate: {
    me: () => ["candidate", "me"] as const,
    byId: (id: number) => ["candidate", "byId", id] as const,
  },
};
