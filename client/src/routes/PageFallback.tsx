// Placeholder while a route's chunk downloads. Shaped like a page so the swap doesn't flash empty.

import { Skeleton } from "@/components/ui/skeleton";

export function PageFallback() {
  return (
    <div className="mx-auto max-w-3xl space-y-4" aria-busy="true">
      <Skeleton className="h-8 w-48 rounded" />
      <Skeleton className="h-36 w-full rounded-lg" />
      <Skeleton className="h-36 w-full rounded-lg" />
    </div>
  );
}
