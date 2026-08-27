-- One period in an alumnus's working life. A timeline rather than columns on the profile: the
-- first job's start date is what "time to first job" measures, and a mutable "current employer"
-- field loses it the day they move on.

CREATE SEQUENCE public.employment_entries_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.employment_entries (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone,
    version integer NOT NULL,
    candidate_profile_id bigint NOT NULL,
    status character varying(255) NOT NULL,
    employer character varying(255),
    job_title character varying(255),
    sector character varying(255),
    city character varying(255),
    started_at date NOT NULL,
    ended_at date,
    last_confirmed_at timestamp(6) without time zone,
    CONSTRAINT employment_entries_status_check CHECK (((status)::text = ANY ((ARRAY['EMPLOYED'::character varying, 'STUDYING'::character varying, 'SEEKING'::character varying])::text[])))
);

ALTER TABLE ONLY public.employment_entries
    ADD CONSTRAINT employment_entries_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.employment_entries
    ADD CONSTRAINT employment_entries_profile_fk FOREIGN KEY (candidate_profile_id) REFERENCES public.candidate_profiles(id);

CREATE INDEX employment_entries_profile_idx ON public.employment_entries USING btree (candidate_profile_id);

-- The report groups by employer, so it reads this column across every row.
CREATE INDEX employment_entries_employer_idx ON public.employment_entries USING btree (employer);
