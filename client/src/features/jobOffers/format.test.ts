import { logoColor, formatMoney, daysFromNow, humanizeType, LOGO_COLORS } from "./format";

test("logoColor picks from the palette by id, wrapping around", () => {
  expect(logoColor(0)).toBe(LOGO_COLORS[0]);
  expect(logoColor(1)).toBe(LOGO_COLORS[1]);
  // wraps: id 6 lands back on index 0 (palette has 6 colors)
  expect(logoColor(LOGO_COLORS.length)).toBe(LOGO_COLORS[0]);
});

test("formatMoney groups thousands", () => {
  expect(formatMoney(1000)).toBe("1,000");
  expect(formatMoney(1500000)).toBe("1,500,000");
});

test("daysFromNow is positive for the future, negative for the past", () => {
  const inTenDays = new Date(Date.now() + 10 * 86_400_000).toISOString();
  const tenDaysAgo = new Date(Date.now() - 10 * 86_400_000).toISOString();
  expect(daysFromNow(inTenDays)).toBe(10);
  expect(daysFromNow(tenDaysAgo)).toBe(-10);
});

test("humanizeType replaces every underscore with a space", () => {
  expect(humanizeType("FULL_TIME")).toBe("FULL TIME");
  expect(humanizeType("COMPUTER_SCIENCE_DEGREE")).toBe("COMPUTER SCIENCE DEGREE");
});
