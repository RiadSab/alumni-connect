// Small presentational helpers shared by the job card and the job detail page.
// Kept in a plain .ts file so both components can import them (and fast refresh
// stays happy).

// Logo background palette from the design, picked by company id.
export const LOGO_COLORS = ["#5645d4", "#dd5b00", "#2a9d99", "#0075de", "#a02e6d", "#1aae39"];

export function logoColor(companyId: number): string {
  return LOGO_COLORS[companyId % LOGO_COLORS.length];
}

export function formatMoney(value: number): string {
  return value.toLocaleString("en-US");
}

// Whole days between now and an ISO date-time. Positive = in the future.
export function daysFromNow(iso: string): number {
  const diff = new Date(iso).getTime() - Date.now();
  return Math.round(diff / 86_400_000);
}

export function humanizeType(value: string): string {
  return value.replace(/_/g, " ");
}
