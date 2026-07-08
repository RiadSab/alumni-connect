import { nonBlank, optionalUrl } from "./validation";
import { registerCandidateSchema } from "@/types/auth";

test("nonBlank rejects empty/whitespace and trims the value", () => {
  expect(nonBlank().safeParse("").success).toBe(false);
  expect(nonBlank().safeParse("   ").success).toBe(false);
  expect(nonBlank().parse("  Ada  ")).toBe("Ada");
});

test("optionalUrl accepts empty, undefined, and http(s) URLs", () => {
  expect(optionalUrl.safeParse("").success).toBe(true);
  expect(optionalUrl.safeParse(undefined).success).toBe(true);
  expect(optionalUrl.safeParse("https://example.com").success).toBe(true);
  expect(optionalUrl.safeParse("http://example.com").success).toBe(true);
});

test("optionalUrl rejects non-http schemes and bare text", () => {
  expect(optionalUrl.safeParse("ftp://example.com").success).toBe(false);
  expect(optionalUrl.safeParse("example.com").success).toBe(false);
});

// a student must supply a studentId; a non-student need not.
const baseCandidate = {
  firstName: "Ada",
  lastName: "Lovelace",
  email: "ada@example.com",
  password: "secret",
  phoneNumber: "0600000000",
  fieldOfStudy: "COMPUTER_SCIENCE",
  graduationYear: 2024,
};

test("student without a studentId fails, flagged on the studentId field", () => {
  const result = registerCandidateSchema.safeParse({ ...baseCandidate, isStudent: true });
  expect(result.success).toBe(false);
  expect(result.error?.issues[0].path).toEqual(["studentId"]);
});

test("student with a studentId passes", () => {
  const result = registerCandidateSchema.safeParse({
    ...baseCandidate,
    isStudent: true,
    studentId: "S12345",
  });
  expect(result.success).toBe(true);
});

test("non-student without a studentId passes", () => {
  const result = registerCandidateSchema.safeParse({ ...baseCandidate, isStudent: false });
  expect(result.success).toBe(true);
});
