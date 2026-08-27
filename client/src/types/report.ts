// Employment report DTOs (admin-only). Rates are fractions, or null when nobody answered.

export interface EmployerCount {
  employer: string;
  count: number;
}

export interface EmploymentReportDTO {
  promotionYear: number;
  totalGraduates: number; // every row the school imported
  claimed: number;
  responded: number; // claimed AND told us something
  employed: number;
  studying: number;
  seeking: number;
  noCurrentPeriod: number; // answered, but every period they gave has ended
  employmentRate: number | null; // employed / responded
  responseRate: number | null; // responded / totalGraduates
  medianMonthsToFirstJob: number | null;
  topEmployers: EmployerCount[];
}
