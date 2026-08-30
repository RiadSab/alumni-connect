// An alumnus opens slots and sees what has been taken (/mentoring/slots). Publishing is refused by
// the server unless the person is on the school's roster.

import { useState } from "react";
import { CalendarClock, Plus, Video } from "lucide-react";
import {
  useCancelSlot,
  useMarkAttendance,
  useMentorBookings,
  useMySlots,
  usePublishSlot,
} from "@/features/mentoring/hooks";
import { useT } from "@/features/i18n/lang-context";
import { isApiError } from "@/lib/http";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { fieldsOptions, mentoringTrackOptions } from "@/types/enums";
import type { MentoringBookingDTO, MentoringSlotDTO } from "@/types/mentoring";

export function MentorSlotsPage() {
  const { t } = useT();
  const slots = useMySlots();

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-foreground">{t("mentorSlots.title")}</h1>
        <p className="mt-1 text-sm text-[var(--color-slate)]">{t("mentorSlots.subtitle")}</p>
      </div>

      <PublishForm />

      <BookedSessions />

      {slots.isLoading ? (
        <Skeleton className="h-24 w-full rounded-lg" />
      ) : !slots.data || slots.data.length === 0 ? (
        <p className="rounded-lg border border-border bg-card px-5 py-8 text-center text-sm text-[var(--color-slate)]">
          {t("mentorSlots.empty")}
        </p>
      ) : (
        <ul className="space-y-3">
          {slots.data.map((slot) => (
            <SlotRow key={slot.id} slot={slot} />
          ))}
        </ul>
      )}
    </div>
  );
}

// Who booked what, with the join link inside the window and the attendance call after it.
function BookedSessions() {
  const { t, lang } = useT();
  const bookings = useMentorBookings();
  const live = (bookings.data ?? []).filter(
    (b) => b.state !== "CANCELLED_BY_STUDENT" && b.state !== "CANCELLED_BY_MENTOR",
  );

  if (live.length === 0) return null;

  return (
    <section className="space-y-3">
      <h2 className="text-sm font-semibold uppercase tracking-wider text-[var(--color-stone)]">
        {t("mentorSlots.sessions")}
      </h2>
      {live.map((booking) => (
        <SessionRow key={booking.id} booking={booking} lang={lang} />
      ))}
    </section>
  );
}

function SessionRow({ booking, lang }: { booking: MentoringBookingDTO; lang: string }) {
  const { t } = useT();
  const attendance = useMarkAttendance();
  const track =
    mentoringTrackOptions.find((o) => o.value === booking.slot.track)?.label ?? booking.slot.track;
  // Read once on mount: a clock read during render is impure, and the attendance button doesn't
  // need to be second-accurate.
  const [now] = useState(() => Date.now());
  const past = new Date(booking.slot.startsAt).getTime() < now;
  const decided = booking.state === "ATTENDED" || booking.state === "NO_SHOW";

  return (
    <article className="flex flex-wrap items-center justify-between gap-4 rounded-lg border border-border bg-card p-4">
      <div className="min-w-0">
        <h3 className="text-base font-semibold text-foreground">{track}</h3>
        <p className="mt-1 text-sm text-[var(--color-steel)]">
          {new Date(booking.slot.startsAt).toLocaleString(lang, {
            dateStyle: "medium",
            timeStyle: "short",
          })}
          {" · "}
          {t("mentorSlots.with", { name: booking.studentName })}
        </p>
      </div>

      <div className="flex shrink-0 flex-wrap items-center gap-2">
        {booking.meetingLink !== null && (
          <Button size="sm" asChild>
            <a href={booking.meetingLink} target="_blank" rel="noreferrer">
              <Video className="size-4" /> {t("mentoring.join")}
            </a>
          </Button>
        )}
        {past && !decided && (
          <>
            <Button
              size="sm"
              variant="outline"
              disabled={attendance.isPending}
              onClick={() => attendance.mutate({ id: booking.id, attended: true })}
            >
              {t("mentorSlots.attended")}
            </Button>
            <Button
              size="sm"
              variant="ghost"
              disabled={attendance.isPending}
              onClick={() => attendance.mutate({ id: booking.id, attended: false })}
            >
              {t("mentorSlots.noShow")}
            </Button>
          </>
        )}
        {decided && (
          <Badge variant="outline">
            {booking.state === "ATTENDED" ? t("mentorSlots.attended") : t("mentorSlots.noShow")}
          </Badge>
        )}
      </div>
    </article>
  );
}

function PublishForm() {
  const { t } = useT();
  const publish = usePublishSlot();
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState({
    startsAt: "",
    track: "DSA",
    targetField: "ANY",
    meetingLink: "",
  });

  function setField(name: keyof typeof form, value: string) {
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  function submit() {
    setError(null);
    if (form.startsAt === "" || form.meetingLink === "") {
      setError(t("mentorSlots.incomplete"));
      return;
    }
    publish.mutate(
      {
        // datetime-local is local wall time; toISOString sends the instant.
        startsAt: new Date(form.startsAt).toISOString(),
        track: form.track,
        targetField: form.targetField === "ANY" ? undefined : form.targetField,
        meetingLink: form.meetingLink.trim(),
      },
      {
        onSuccess: () => setForm({ ...form, startsAt: "" }),
        onError: (e) => setError(isApiError(e) ? e.message : t("mentorSlots.error")),
      },
    );
  }

  return (
    <section className="flex flex-col gap-4 rounded-lg border border-border bg-card p-5">
      <h2 className="text-sm font-semibold text-foreground">{t("mentorSlots.open")}</h2>

      <div className="grid gap-4 sm:grid-cols-2">
        <div className="flex flex-col gap-2">
          <label htmlFor="startsAt" className="text-sm font-medium">Date and time</label>
          <Input
            id="startsAt"
            type="datetime-local"
            value={form.startsAt}
            onChange={(event) => setField("startsAt", event.target.value)}
          />
        </div>
        <div className="flex flex-col gap-2">
          <label className="text-sm font-medium">Track</label>
          <Select value={form.track} onValueChange={(v) => setField("track", v)}>
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {mentoringTrackOptions.map((option) => (
                <SelectItem key={option.value} value={option.value}>
                  {option.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      <div className="flex flex-col gap-2">
        <label className="text-sm font-medium">Open to</label>
        <Select value={form.targetField} onValueChange={(v) => setField("targetField", v)}>
          <SelectTrigger>
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ANY">{t("mentorSlots.anyField")}</SelectItem>
            {fieldsOptions.map((option) => (
              <SelectItem key={option.value} value={option.value}>
                {option.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div className="flex flex-col gap-2">
        <label htmlFor="meetingLink" className="text-sm font-medium">Meeting link</label>
        <Input
          id="meetingLink"
          type="url"
          placeholder="https://meet.google.com/abc-defg-hij"
          value={form.meetingLink}
          onChange={(event) => setField("meetingLink", event.target.value)}
        />
        <span className="text-xs text-[var(--color-slate)]">{t("mentorSlots.linkHelp")}</span>
      </div>

      {error !== null && <p className="text-sm text-destructive">{error}</p>}

      <div>
        <Button onClick={submit} disabled={publish.isPending}>
          <Plus className="size-4" />
          {publish.isPending ? t("mentorSlots.publishing") : t("mentorSlots.publish")}
        </Button>
      </div>
    </section>
  );
}

function SlotRow({ slot }: { slot: MentoringSlotDTO }) {
  const { t, lang } = useT();
  const cancel = useCancelSlot();
  const track = mentoringTrackOptions.find((o) => o.value === slot.track)?.label ?? slot.track;

  return (
    <li className="flex flex-wrap items-center justify-between gap-4 rounded-lg border border-border bg-card p-4">
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <h3 className="text-base font-semibold text-foreground">{track}</h3>
          <Badge variant={slot.status === "BOOKED" ? "default" : "outline"}>
            {slot.status === "BOOKED"
              ? t("mentorSlots.booked")
              : slot.status === "CANCELLED"
                ? t("mentorSlots.cancelled")
                : t("mentorSlots.openStatus")}
          </Badge>
        </div>
        <p className="mt-1 inline-flex items-center gap-1.5 text-sm text-[var(--color-steel)]">
          <CalendarClock className="size-4" />
          {new Date(slot.startsAt).toLocaleString(lang, { dateStyle: "full", timeStyle: "short" })}
          {" · "}
          {t("mentoring.minutes", { n: slot.durationMinutes })}
        </p>
      </div>
      {slot.status !== "CANCELLED" && (
        <Button
          size="sm"
          variant="ghost"
          onClick={() => cancel.mutate(slot.id)}
          disabled={cancel.isPending}
        >
          {t("mentorSlots.cancel")}
        </Button>
      )}
    </li>
  );
}
