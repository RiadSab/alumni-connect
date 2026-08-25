-- Interview details, filled in when an application moves to SCHEDULED_INTERVIEW.
-- interview_at is timestamptz: the candidate may well read it from another timezone.

ALTER TABLE public.job_applications
    ADD COLUMN interview_mode character varying(255),
    ADD COLUMN interview_at timestamp(6) with time zone,
    ADD COLUMN interview_link character varying(255),
    ADD COLUMN interview_location character varying(255),
    ADD COLUMN interviewer_name character varying(255);

ALTER TABLE public.job_applications
    ADD CONSTRAINT job_applications_interview_mode_check
    CHECK (((interview_mode)::text = ANY ((ARRAY['ONLINE'::character varying, 'ONSITE'::character varying])::text[])));
