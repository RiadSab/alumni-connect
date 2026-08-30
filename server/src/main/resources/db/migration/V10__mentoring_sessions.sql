-- The session lifecycle: a confirmation link 24h out, a reminder an hour out, and the two
-- notification types the booking flow raises.

ALTER TABLE public.mentoring_bookings
    ADD COLUMN confirm_token_hash character varying(255),
    ADD COLUMN confirm_sent_at timestamp(6) without time zone,
    ADD COLUMN reminded_at timestamp(6) without time zone;

-- One live token per booking, so it lives on the row instead of in a third token table.
CREATE UNIQUE INDEX mentoring_bookings_confirm_token_idx
    ON public.mentoring_bookings USING btree (confirm_token_hash);

ALTER TABLE public.notifications
    DROP CONSTRAINT notifications_type_check;

ALTER TABLE public.notifications
    ADD CONSTRAINT notifications_type_check CHECK (((type)::text = ANY ((ARRAY[
        'APPLICATION_ACCEPTED'::character varying,
        'APPLICATION_REJECTED'::character varying,
        'INTERVIEW_SCHEDULED'::character varying,
        'MENTORING_SLOT_BOOKED'::character varying,
        'MENTORING_SESSION_CANCELLED'::character varying,
        'MENTORING_SESSION_RELEASED'::character varying])::text[])));
