// Public claim page at /claim/:token — the link from the school's invitation email.

import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { KeyRound, Phone } from "lucide-react";
import { useClaimAccount, useClaimDetails, useOptOut } from "@/features/alumni/hooks";
import { AuthScreen } from "@/features/auth/AuthScreen";
import { AuthBrand, AuthField, AuthPasswordField } from "@/features/auth/fields";
import { useT } from "@/features/i18n/lang-context";
import { claimAccountSchema } from "@/types/alumni";
import { isApiError } from "@/lib/http";
import { fieldsOptions } from "@/types/enums";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";

const cardClass = "w-full max-w-sm bg-card/95 shadow-[var(--shadow-2)] backdrop-blur-sm";

export function ClaimAccountPage() {
  const { t } = useT();
  const params = useParams<{ token: string }>();
  const token = params.token ?? "";

  const details = useClaimDetails(token);
  const claim = useClaimAccount();
  const optOut = useOptOut();

  const [password, setPassword] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [error, setError] = useState<string | null>(null);

  if (details.isLoading) {
    return (
      <AuthScreen>
        <Card className={cardClass}>
          <CardContent className="flex flex-col gap-4 p-7">
            <Skeleton className="h-8 w-40 rounded" />
            <Skeleton className="h-24 w-full rounded" />
          </CardContent>
        </Card>
      </AuthScreen>
    );
  }

  if (details.isError || !details.data) {
    return (
      <Outcome title={t("claim.invalidTitle")} subtitle={t("claim.invalidSubtitle")} />
    );
  }

  if (optOut.isSuccess) {
    return <Outcome title={t("claim.optOutDoneTitle")} subtitle={t("claim.optOutDoneSubtitle")} />;
  }

  if (claim.isSuccess) {
    return (
      <Outcome
        title={t("claim.doneTitle")}
        subtitle={t("claim.doneSubtitle", { email: details.data.email })}
        action={{ to: "/login", label: t("claim.signIn") }}
      />
    );
  }

  const graduate = details.data;
  const field = fieldsOptions.find((o) => o.value === graduate.fieldOfStudy)?.label ?? graduate.fieldOfStudy;

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setError(null);
    const result = claimAccountSchema.safeParse({ token, password, phoneNumber });
    if (!result.success) {
      setError(t("claim.passwordRequired"));
      return;
    }
    claim.mutate(result.data, {
      onError: (e) => setError(isApiError(e) ? e.message : t("claim.error")),
    });
  }

  return (
    <AuthScreen>
      <Card className={cardClass}>
        <CardContent className="flex flex-col gap-6 p-7">
          <AuthBrand
            title={t("claim.title")}
            subtitle={`${graduate.firstName} ${graduate.lastName} — ${t("claim.graduate", { field, year: graduate.promotionYear })}`}
          />

          <p className="-mt-2 text-sm text-muted-foreground">{t("claim.subtitle")}</p>

          <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
            <AuthPasswordField
              id="password"
              label={t("claim.password")}
              icon={KeyRound}
              value={password}
              autoComplete="new-password"
              onChange={(event) => setPassword(event.target.value)}
              error={error ?? undefined}
            />

            <AuthField
              id="phoneNumber"
              label={t("claim.phone")}
              icon={Phone}
              type="tel"
              value={phoneNumber}
              autoComplete="tel"
              onChange={(event) => setPhoneNumber(event.target.value)}
            />

            <Button type="submit" className="h-10" disabled={claim.isPending}>
              {claim.isPending ? t("claim.submitting") : t("claim.submit")}
            </Button>
          </form>

          <div className="border-t border-border pt-4">
            <p className="text-xs text-muted-foreground">{t("claim.why")}</p>
            <OptOutDialog
              onConfirm={() => optOut.mutate(token)}
              pending={optOut.isPending}
              error={optOut.isError ? t("claim.optOutError") : null}
            />
          </div>
        </CardContent>
      </Card>
    </AuthScreen>
  );
}

function OptOutDialog({
  onConfirm,
  pending,
  error,
}: {
  onConfirm: () => void;
  pending: boolean;
  error: string | null;
}) {
  const { t } = useT();
  return (
    <Dialog>
      <DialogTrigger asChild>
        <Button variant="ghost" size="sm" className="mt-2 px-0 text-sm text-muted-foreground">
          {t("claim.optOut")}
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogTitle>{t("claim.optOutTitle")}</DialogTitle>
        <DialogDescription>{t("claim.optOutPrompt")}</DialogDescription>
        {error && <p className="mt-2 text-sm text-destructive">{error}</p>}
        <DialogFooter>
          <DialogClose asChild>
            <Button variant="ghost" size="sm">
              {t("claim.optOutCancel")}
            </Button>
          </DialogClose>
          <Button variant="destructive" size="sm" disabled={pending} onClick={onConfirm}>
            {t("claim.optOutConfirm")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function Outcome({
  title,
  subtitle,
  action,
}: {
  title: string;
  subtitle: string;
  action?: { to: string; label: string };
}) {
  return (
    <AuthScreen>
      <Card className={cardClass}>
        <CardContent className="flex flex-col gap-6 p-7">
          <AuthBrand title={title} subtitle={subtitle} />
          {action && (
            <Button asChild className="h-10 w-full">
              <Link to={action.to}>{action.label}</Link>
            </Button>
          )}
        </CardContent>
      </Card>
    </AuthScreen>
  );
}
