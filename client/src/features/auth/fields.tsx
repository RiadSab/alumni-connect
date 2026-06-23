import { useState, type ComponentProps } from "react";
import { Eye, EyeOff, type LucideIcon } from "lucide-react";
import { useT } from "@/features/i18n/lang-context";
import { cn } from "@/lib/utils";
import { Input } from "@/components/ui/input";

// Centered brand mark + heading, shared by the public auth cards.
export function AuthBrand({ title, subtitle }: { title: string; subtitle: string }) {
  return (
    <div className="flex flex-col items-center gap-3 text-center">
      <img
        src="/logo_blue_background.png"
        alt="Alumni Connect"
        className="size-12 rounded-xl object-cover shadow-[var(--shadow-1)]"
      />
      <div className="space-y-1">
        <h1 className="text-xl font-semibold tracking-tight">{title}</h1>
        <p className="text-sm text-muted-foreground">{subtitle}</p>
      </div>
    </div>
  );
}

type FieldProps = {
  id: string;
  label: string;
  icon?: LucideIcon;
  error?: string;
} & ComponentProps<"input">;

// Label + icon input + error trio.
export function AuthField({ id, label, icon: Icon, error, className, ...props }: FieldProps) {
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-sm font-medium">
        {label}
      </label>
      <div className="relative">
        {Icon && (
          <Icon className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        )}
        <Input
          id={id}
          className={cn("h-10", Icon && "pl-9", className)}
          aria-invalid={error !== undefined}
          {...props}
        />
      </div>
      {error && <p className="text-sm text-destructive">{error}</p>}
    </div>
  );
}

// Password field with an icon and a show/hide toggle.
export function AuthPasswordField({ id, label, icon: Icon, error, ...props }: FieldProps) {
  const { t } = useT();
  const [show, setShow] = useState(false);
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-sm font-medium">
        {label}
      </label>
      <div className="relative">
        {Icon && (
          <Icon className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        )}
        <Input
          id={id}
          type={show ? "text" : "password"}
          className={cn("h-10 pr-9", Icon && "pl-9")}
          aria-invalid={error !== undefined}
          {...props}
        />
        <button
          type="button"
          onClick={() => setShow((shown) => !shown)}
          aria-label={show ? t("auth.login.hidePassword") : t("auth.login.showPassword")}
          className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground transition-colors hover:text-foreground"
        >
          {show ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
        </button>
      </div>
      {error && <p className="text-sm text-destructive">{error}</p>}
    </div>
  );
}
