// Cross-cutting shapes shared by every feature (§"Conventions" in the API reference).

import type { QueryValue } from "@/lib/http";

/** Spring `Page<T>` envelope returned by every paginated list endpoint. */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // 0-based current page index
  size: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
  sort: { sorted: boolean; empty: boolean; unsorted: boolean };
}

/**
 * Query params accepted by any paginated endpoint. `sort` e.g. "createdAt,desc".
 * The index signature lets feature filter types (which extend this) be passed
 * straight to http's `query` option.
 */
export interface PageParams {
  page?: number;
  size?: number;
  sort?: string | string[];
  [key: string]: QueryValue;
}

/** Audit fields present on most output DTOs (ISO-8601 date-time strings). */
export interface AuditFields {
  createdAt: string;
  updatedAt: string;
}

/** Returned by every upload endpoint (resume, photo, logo). */
export interface StoredFileDTO {
  storageId: string;
  originalFilename: string;
  contentType: string;
  size: number;
}
