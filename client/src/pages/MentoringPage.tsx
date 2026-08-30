// Students browse open mock-interview slots and book one (/mentoring). The quota is shown up front
// because it is the thing that decides whether booking is even possible.

import { useState } from "react";
import { CalendarClock, Video } from "lucide-react";
import {
  useBookSlot,
  useCancelBooking,
  useMyBookings,
  useOpenSlots,
  useRemainingBookings,
} from "@/features/mentoring/hooks";
import { useT } from "@/features/i18n/lang-context";
import { isApiError } from "@/lib/http";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { fieldsOptions, mentoringTrackOptions } from "@/types/enums";
import type { Fields, MentoringTrack } from "@/types/enums";
import type { MentoringBookingDTO, MentoringSlotDTO } from "@/types/mentoring";

// A booking still worth showing at the top of the page.
const LIVE: MentoringBookingDTO["state"][] = ["BOOKED", "CONFIRMED"];

export function MentoringPage() {
  const { t } = useT();
  const [track, setTrack] = useState<MentoringTrack | "ALL">("ALL");
  const [field, setField] = useState<Fields | "ALL">("ALL");
  const [error, setError] = useState<string | null>(null);

  const slots = useOpenSlots({
    track: track === "ALL" ? undefined : track,
    field: field === "ALL" ? undefined : field,
  });
  const bookings = useMyBookings();
  const remaining = useRemainingBookings();
  const book = useBookSlot();

  const live = (bookings.data ?? []).filter((b) => LIVE.includes(b.state));
  const left = remaining.data?.remaining ?? 0;

  function onBook(slotId: number) {
    setError(null);
    book.mutate(slotId, {
      onError: (e) => setError(isApiError(e) ? e.message : t("mentoring.bookError")),
    });
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-foreground">{t("mentoring.title")}</h1>
        <p className="mt-1 text-sm text-[var(--color-slate)]">{t("mentoring.subtitle")}</p>
      </div>

      <p className="rounded-lg border border-border bg-[var(--color-tint-sky)] px-4 py-3 text-sm font-medium text-foreground">
        {t("mentoring.remaining", { n: left })}
      </p>

      {live.length > 0 && (
        <section className="space-y-3">
          <h2 className="text-sm font-semibold uppercase tracking-wider text-[var(--color-stone)]">
            {t("mentoring.mySessions")}
          </h2>
          {live.map((booking) => (
            <BookingRow key={booking.id} booking={booking} />
          ))}
        </section>
      )}

      <div className="flex flex-wrap items-center gap-3">
        <Select value={track} onValueChange={(v) => setTrack(v as MentoringTrack | "ALL")}>
          <SelectTrigger className="min-w-48">
            <SelectValue placeholder={t("mentoring.track")} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">{t("mentoring.allTracks")}</SelectItem>
            {mentoringTrackOptions.map((option) => (
              <SelectItem key={option.value} value={option.value}>
                {option.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        <Select value={field} onValueChange={(v) => setField(v as Fields | "ALL")}>
          <SelectTrigger className="min-w-48">
            <SelectValue placeholder={t("mentoring.field")} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">{t("mentoring.allFields")}</SelectItem>
            {fieldsOptions.map((option) => (
              <SelectItem key={option.value} value={option.value}>
                {option.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {error !== null && <p className="text-sm text-destructive">{error}</p>}

      {slots.isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-20 w-full rounded-lg" />
          ))}
        </div>
      ) : !slots.data || slots.data.empty ? (
        <p className="rounded-lg border border-border bg-card px-5 py-8 text-center text-sm text-[var(--color-slate)]">
          {t("mentoring.empty")}
        </p>
      ) : (
        <ul className="space-y-3">
          {slots.data.content.map((slot) => (
            <SlotRow
              key={slot.id}
              slot={slot}
              disabled={left === 0 || book.isPending}
              onBook={() => onBook(slot.id)}
            />
          ))}
        </ul>
      )}
    </div>
  );
}

function SlotRow({
  slot,
  disabled,
  onBook,
}: {
  slot: MentoringSlotDTO;
  disabled: boolean;
  onBook: () => void;
}) {
  const { t, lang } = useT();
  const track = mentoringTrackOptions.find((o) => o.value === slot.track)?.label ?? slot.track;
  const field = slot.targetField
    ? fieldsOptions.find((o) => o.value === slot.targetField)?.label ?? slot.targetField
    : null;

  return (
    <li className="flex flex-wrap items-center justify-between gap-4 rounded-lg border border-border bg-card p-4">
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <h3 className="text-base font-semibold leading-tight text-foreground">{track}</h3>
          {field !== null && <Badge variant="outline">{field}</Badge>}
        </div>
        <p className="mt-1 inline-flex items-center gap-1.5 text-sm text-[var(--color-steel)]">
          <CalendarClock className="size-4" />
          {new Date(slot.startsAt).toLocaleString(lang, { dateStyle: "full", timeStyle: "short" })}
          {" · "}
          {t("mentoring.minutes", { n: slot.durationMinutes })}
        </p>
        <p className="mt-0.5 text-sm text-[var(--color-slate)]">
          {slot.mentorName}
          {slot.mentorPromotion !== null && ` · ${t("mentoring.promotion", { n: slot.mentorPromotion })}`}
        </p>
      </div>
      <Button size="sm" onClick={onBook} disabled={disabled}>
        {t("mentoring.book")}
      </Button>
    </li>
  );
}

function BookingRow({ booking }: { booking: MentoringBookingDTO }) {
  const { t, lang } = useT();
  const cancel = useCancelBooking();
  const track =
    mentoringTrackOptions.find((o) => o.value === booking.slot.track)?.label ?? booking.slot.track;

  return (
    <article className="flex flex-wrap items-center justify-between gap-4 rounded-lg border border-[var(--color-brand-purple-800)] bg-[var(--color-tint-lavender)] p-4">
      <div className="min-w-0">
        <h3 className="text-base font-semibold text-foreground">{track}</h3>
        <p className="mt-1 text-sm text-[var(--color-charcoal)]">
          {new Date(booking.slot.startsAt).toLocaleString(lang, {
            dateStyle: "full",
            timeStyle: "short",
          })}
          {" · "}
          {booking.slot.mentorName}
        </p>
      </div>
      <div className="flex shrink-0 items-center gap-2">
        {booking.meetingLink === null && (
          <span className="text-xs text-[var(--color-slate)]">{t("mentoring.linkNote")}</span>
        )}
        {booking.meetingLink !== null && (
          <Button size="sm" variant="outline" asChild>
            <a href={booking.meetingLink} target="_blank" rel="noreferrer">
              <Video className="size-4" /> {t("mentoring.join")}
            </a>
          </Button>
        )}
        <Button
          size="sm"
          variant="ghost"
          onClick={() => cancel.mutate(booking.id)}
          disabled={cancel.isPending}
        >
          {t("mentoring.cancel")}
        </Button>
      </div>
    </article>
  );
}
