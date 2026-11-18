import { afterEach, beforeEach, expect, test, vi } from "vitest";
import { ApiError, http } from "./http";
import { getToken, getStoredUser, setStoredUser, setToken } from "./auth";

const fetchMock = vi.fn<typeof fetch>();

function json(status: number, body: unknown, headers: Record<string, string> = {}) {
  return new Response(JSON.stringify(body), {
    status, headers: { "Content-Type": "application/json", ...headers },
  });
}

beforeEach(() => {
  localStorage.clear();
  fetchMock.mockReset();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => vi.unstubAllGlobals());

test("429 exposes Retry-After and does not retry or discard credentials", async () => {
  setToken("access");
  fetchMock.mockResolvedValue(json(429, {
    status: 429, code: "RATE_LIMITED", message: "Too many requests.",
  }, { "Retry-After": "15" }));

  await expect(http.post("/candidates/me/resume")).rejects.toMatchObject({
    status: 429, code: "RATE_LIMITED", retryAfterSeconds: 15,
  });
  expect(fetchMock).toHaveBeenCalledTimes(1);
  expect(getToken()).toBe("access");
});

test("throttled refresh preserves the stored session and propagates 429", async () => {
  setToken("expired-access");
  setStoredUser({ userId: 42, email: "alice@example.com", firstName: "Alice", lastName: "A", userType: "CANDIDATE" });
  fetchMock.mockResolvedValueOnce(json(401, { status: 401, code: "TOKEN_EXPIRED" }))
    .mockResolvedValueOnce(json(429, { status: 429, code: "RATE_LIMITED" }, { "Retry-After": "30" }));

  await expect(http.get("/candidates/me")).rejects.toMatchObject({ status: 429, retryAfterSeconds: 30 });
  expect(fetchMock).toHaveBeenCalledTimes(2);
  expect(getToken()).toBe("expired-access");
  expect(getStoredUser()?.userId).toBe(42);
});

test("a later request can refresh successfully after a temporary throttle", async () => {
  setToken("expired-access");
  fetchMock.mockResolvedValueOnce(json(401, { status: 401, code: "TOKEN_EXPIRED" }))
    .mockResolvedValueOnce(json(429, { status: 429, code: "RATE_LIMITED" }));
  await expect(http.get("/candidates/me")).rejects.toBeInstanceOf(ApiError);

  fetchMock.mockResolvedValueOnce(json(401, { status: 401, code: "TOKEN_EXPIRED" }))
    .mockResolvedValueOnce(json(200, { token: "new-access" }))
    .mockResolvedValueOnce(json(200, { email: "alice@example.com" }));
  await expect(http.get("/candidates/me")).resolves.toEqual({ email: "alice@example.com" });
  expect(getToken()).toBe("new-access");
});

test("temporary refresh server failure preserves credentials", async () => {
  setToken("expired-access");
  fetchMock.mockResolvedValueOnce(json(401, { status: 401, code: "TOKEN_EXPIRED" }))
    .mockResolvedValueOnce(json(503, { status: 503, message: "Unavailable" }));
  await expect(http.get("/candidates/me")).rejects.toMatchObject({ status: 503 });
  expect(getToken()).toBe("expired-access");
});

test("concurrent expired requests share a throttled refresh", async () => {
  setToken("expired-access");
  fetchMock.mockImplementation(async (input) => String(input).endsWith("/auth/refresh")
    ? json(429, { status: 429, code: "RATE_LIMITED" })
    : json(401, { status: 401, code: "TOKEN_EXPIRED" }));
  const results = await Promise.allSettled([
    http.get("/candidates/me"), http.get("/notifications"),
  ]);
  expect(results.every((result) => result.status === "rejected"
    && result.reason instanceof ApiError && result.reason.status === 429)).toBe(true);
  expect(fetchMock.mock.calls.filter(([url]) => String(url).endsWith("/auth/refresh"))).toHaveLength(1);
  expect(getToken()).toBe("expired-access");
});

test("an invalid refresh credential still ends the local session", async () => {
  // Avoid triggering navigation while checking the actual credential-clearing behavior.
  const previousPath = window.location.pathname;
  window.history.replaceState(null, "", "/login");
  setToken("expired-access");
  setStoredUser({ userId: 42, email: "alice@example.com", firstName: "Alice", lastName: "A", userType: "CANDIDATE" });
  fetchMock.mockResolvedValueOnce(json(401, { status: 401, code: "TOKEN_EXPIRED" }))
    .mockResolvedValueOnce(json(401, { status: 401 }));
  try {
    await expect(http.get("/candidates/me")).rejects.toMatchObject({ status: 401 });
    expect(getToken()).toBeNull();
    expect(getStoredUser()).toBeNull();
  } finally {
    window.history.replaceState(null, "", previousPath);
  }
});

test("Retry-After HTTP dates are parsed and invalid values are ignored", async () => {
  const deadline = new Date(Date.now() + 60_000).toUTCString();
  fetchMock.mockResolvedValueOnce(json(429, { status: 429 }, { "Retry-After": deadline }));
  try {
    await http.get("/job-offers");
    throw new Error("Expected rate limiting");
  } catch (error) {
    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).retryAfterSeconds).toBeGreaterThanOrEqual(59);
    expect((error as ApiError).retryAfterSeconds).toBeLessThanOrEqual(60);
  }
  fetchMock.mockResolvedValueOnce(json(429, { status: 429 }, { "Retry-After": "invalid" }));
  await expect(http.get("/job-offers")).rejects.toMatchObject({ retryAfterSeconds: undefined });
});
