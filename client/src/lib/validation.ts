// Shared zod building blocks for request DTOs.

import { z } from "zod";

/** Trimmed, non-empty string — mirrors the backend's @NotBlank. */
export const nonBlank = (message = "Required") => z.string().trim().min(1, message);

/** Optional URL that must be empty or start with http(s):// (matches backend rule). */
export const optionalUrl = z
  .union([
    z.literal(""),
    z.string().trim().regex(/^https?:\/\/.+/, "Must start with http:// or https://"),
  ])
  .optional();
