// A company's logo, falling back to its initial on a colored tile when there's no logo.

import { companiesApi } from "@/api/companies";
import { cn } from "@/lib/utils";
import { logoColor } from "@/features/jobOffers/format";

interface CompanyLogoProps {
  companyId: number;
  companyName: string;
  logoId: string | null;
  className?: string; // tile box: size + rounding
  textClassName?: string; // initial-letter size for the fallback
}

export function CompanyLogo({ companyId, companyName, logoId, className, textClassName }: CompanyLogoProps) {
  if (logoId) {
    return (
      <img
        src={`${companiesApi.logoUrl(companyId)}?v=${logoId}`}
        alt={companyName}
        className={cn("shrink-0 object-cover", className)}
      />
    );
  }

  return (
    <div
      className={cn("grid shrink-0 place-items-center font-bold text-white", className, textClassName)}
      style={{ background: logoColor(companyId) }}
    >
      {companyName.charAt(0)}
    </div>
  );
}
