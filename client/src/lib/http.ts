// Single HTTP chokepoint for the whole client.
//
//  - Prefixes every path with the API base ("/api" by default; override with
//    VITE_API_BASE_URL).
//  - Injects the JWT as `Authorization: Bearer <token>`.
//  - Serializes query params (arrays repeat the key: ?skills=a&skills=b).
//  - Sends JSON or multipart bodies; reads JSON, text, or binary responses.
//  - Normalizes every failure into a typed `ApiError` matching the backend's
//    error envelope (shared/exception/ApiError.java).

import { getToken } from "@/lib/auth";

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "/api";

/** Absolute API URL for a path — use for public image `src` (no auth needed). */
export const apiUrl = (path: string): string => `${BASE_URL}${path}`;

/** Stable, machine-readable codes the backend sets on the error envelope. */
export type ApiErrorCode =
  | "AUTH_REQUIRED"
  | "TOKEN_EXPIRED"
  | "INVALID_TOKEN"
  | "ACCESS_DENIED"
  | "ACCOUNT_DISABLED"
  // plus HttpStatus names for everything else: "NOT_FOUND", "CONFLICT", ...
  | (string & {});

/** JSON body shape of every error response (ApiError.java). */
export interface ApiErrorBody {
  timestamp?: string;
  status: number;
  error?: string;
  code?: ApiErrorCode;
  message?: string;
  fieldErrors?: Record<string, string[]>;
}

/** Error thrown by every http.* call on a non-2xx response. */
export class ApiError extends Error {
  readonly status: number;
  readonly code?: ApiErrorCode;
  readonly fieldErrors?: Record<string, string[]>;
  readonly body?: ApiErrorBody;

  constructor(status: number, message: string, body?: ApiErrorBody) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = body?.code;
    this.fieldErrors = body?.fieldErrors;
    this.body = body;
  }
}

export function isApiError(err: unknown): err is ApiError {
  return err instanceof ApiError;
}

type QueryPrimitive = string | number | boolean;
export type QueryValue = QueryPrimitive | QueryPrimitive[] | null | undefined;

export interface RequestOptions {
  query?: Record<string, QueryValue>;
  /** JSON request body (serialized + Content-Type set). */
  json?: unknown;
  /** Multipart body; Content-Type/boundary is set by the browser. */
  form?: FormData;
  headers?: Record<string, string>;
  signal?: AbortSignal;
}

interface FullOptions extends RequestOptions {
  method: string;
}

function buildUrl(path: string, query?: Record<string, QueryValue>): string {
  const url = `${BASE_URL}${path}`;
  if (!query) return url;

  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value === null || value === undefined) continue;
    if (Array.isArray(value)) {
      for (const item of value) search.append(key, String(item));
    } else {
      search.append(key, String(value));
    }
  }
  const qs = search.toString();
  return qs ? `${url}?${qs}` : url;
}

async function send(path: string, options: FullOptions): Promise<Response> {
  const headers: Record<string, string> = { ...options.headers };

  const token = getToken();
  if (token) headers["Authorization"] = `Bearer ${token}`;

  let body: BodyInit | undefined;
  if (options.form) {
    body = options.form; // browser sets multipart Content-Type + boundary
  } else if (options.json !== undefined) {
    headers["Content-Type"] = "application/json";
    body = JSON.stringify(options.json);
  }

  const res = await fetch(buildUrl(path, options.query), {
    method: options.method,
    headers,
    body,
    signal: options.signal,
  });

  if (!res.ok) throw await toApiError(res);
  return res;
}

async function toApiError(res: Response): Promise<ApiError> {
  let body: ApiErrorBody | undefined;
  try {
    if (res.headers.get("content-type")?.includes("application/json")) {
      body = (await res.json()) as ApiErrorBody;
    }
  } catch {
    // non-JSON / empty error body — fall back to status text below
  }
  const message = body?.message ?? res.statusText ?? `HTTP ${res.status}`;
  return new ApiError(res.status, message, body);
}

/** Parse a JSON response; tolerate empty bodies (e.g. 201/200 with no content). */
async function parseJson<T>(res: Response): Promise<T> {
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

export const http = {
  get: <T>(path: string, options?: RequestOptions) =>
    send(path, { ...options, method: "GET" }).then(parseJson<T>),

  post: <T>(path: string, options?: RequestOptions) =>
    send(path, { ...options, method: "POST" }).then(parseJson<T>),

  patch: <T>(path: string, options?: RequestOptions) =>
    send(path, { ...options, method: "PATCH" }).then(parseJson<T>),

  put: <T>(path: string, options?: RequestOptions) =>
    send(path, { ...options, method: "PUT" }).then(parseJson<T>),

  del: <T>(path: string, options?: RequestOptions) =>
    send(path, { ...options, method: "DELETE" }).then(parseJson<T>),

  /** Plain-text response (e.g. admin lifecycle confirmation strings). */
  getText: (path: string, options?: RequestOptions) =>
    send(path, { ...options, method: "GET" }).then((res) => res.text()),

  postText: (path: string, options?: RequestOptions) =>
    send(path, { ...options, method: "POST" }).then((res) => res.text()),

  /** Binary stream response (resume / photo / logo downloads). */
  getBlob: (path: string, options?: RequestOptions) =>
    send(path, { ...options, method: "GET" }).then((res) => res.blob()),
};
