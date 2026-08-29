-- Mentoring: an alumnus publishes interview slots, a student books one.
-- A slot is supply; a booking is one attempt at it. They are separate tables because a released or
-- cancelled booking has to outlive the slot returning to the pool — the semester cap and the
-- no-show count are read from those rows. See docs/decisions.md.

CREATE SEQUENCE public.mentoring_slots_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.mentoring_slots (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone,
    version integer NOT NULL,
    mentor_profile_id bigint NOT NULL,
    starts_at timestamp(6) with time zone NOT NULL,
    duration_minutes integer NOT NULL,
    track character varying(255) NOT NULL,
    target_field character varying(255),
    meeting_link character varying(512) NOT NULL,
    status character varying(255) NOT NULL,
    CONSTRAINT mentoring_slots_track_check CHECK (((track)::text = ANY ((ARRAY['DSA'::character varying, 'SYSTEM_DESIGN'::character varying, 'BEHAVIORAL'::character varying, 'FRENCH_PRACTICE'::character varying, 'ENGLISH_PRACTICE'::character varying])::text[]))),
    CONSTRAINT mentoring_slots_target_field_check CHECK (((target_field)::text = ANY ((ARRAY['COMPUTER_SCIENCE'::character varying, 'INFORMATION_TECHNOLOGY'::character varying, 'SOFTWARE_ENGINEERING'::character varying, 'DATA_SCIENCE'::character varying, 'CYBER_SECURITY'::character varying, 'NETWORKING'::character varying, 'ARTIFICIAL_INTELLIGENCE'::character varying, 'MACHINE_LEARNING'::character varying, 'WEB_DEVELOPMENT'::character varying, 'MOBILE_DEVELOPMENT'::character varying, 'CLOUD_COMPUTING'::character varying, 'AEROSPACE_ENGINEERING'::character varying, 'MECHANICAL_ENGINEERING'::character varying, 'CIVIL_ENGINEERING'::character varying, 'ELECTRICAL_ENGINEERING'::character varying, 'CHEMICAL_ENGINEERING'::character varying, 'BIOTECHNOLOGY'::character varying, 'ENVIRONMENTAL_ENGINEERING'::character varying, 'INDUSTRIAL_ENGINEERING'::character varying, 'MATERIALS_SCIENCE'::character varying, 'PHYSICS'::character varying, 'MATHEMATICS'::character varying, 'STATISTICS'::character varying, 'CHEMISTRY'::character varying, 'BIOLOGY'::character varying, 'ECONOMICS'::character varying, 'BUSINESS_ADMINISTRATION'::character varying])::text[]))),
    CONSTRAINT mentoring_slots_status_check CHECK (((status)::text = ANY ((ARRAY['OPEN'::character varying, 'BOOKED'::character varying, 'CANCELLED'::character varying])::text[])))
);

ALTER TABLE ONLY public.mentoring_slots
    ADD CONSTRAINT mentoring_slots_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.mentoring_slots
    ADD CONSTRAINT mentoring_slots_mentor_fk FOREIGN KEY (mentor_profile_id) REFERENCES public.candidate_profiles(id);

-- Browsing is always "open slots, soonest first".
CREATE INDEX mentoring_slots_open_idx ON public.mentoring_slots USING btree (status, starts_at);
CREATE INDEX mentoring_slots_mentor_idx ON public.mentoring_slots USING btree (mentor_profile_id);

CREATE SEQUENCE public.mentoring_bookings_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.mentoring_bookings (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone,
    version integer NOT NULL,
    slot_id bigint NOT NULL,
    student_profile_id bigint NOT NULL,
    state character varying(255) NOT NULL,
    confirmed_at timestamp(6) without time zone,
    cancelled_at timestamp(6) without time zone,
    CONSTRAINT mentoring_bookings_state_check CHECK (((state)::text = ANY ((ARRAY['BOOKED'::character varying, 'CONFIRMED'::character varying, 'RELEASED_UNCONFIRMED'::character varying, 'CANCELLED_BY_STUDENT'::character varying, 'CANCELLED_BY_MENTOR'::character varying, 'ATTENDED'::character varying, 'NO_SHOW'::character varying])::text[])))
);

ALTER TABLE ONLY public.mentoring_bookings
    ADD CONSTRAINT mentoring_bookings_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.mentoring_bookings
    ADD CONSTRAINT mentoring_bookings_slot_fk FOREIGN KEY (slot_id) REFERENCES public.mentoring_slots(id);

ALTER TABLE ONLY public.mentoring_bookings
    ADD CONSTRAINT mentoring_bookings_student_fk FOREIGN KEY (student_profile_id) REFERENCES public.candidate_profiles(id);

-- The semester cap counts a student's bookings; the slot side reads "is this one still live?".
CREATE INDEX mentoring_bookings_student_idx ON public.mentoring_bookings USING btree (student_profile_id, state);
CREATE INDEX mentoring_bookings_slot_idx ON public.mentoring_bookings USING btree (slot_id, state);
