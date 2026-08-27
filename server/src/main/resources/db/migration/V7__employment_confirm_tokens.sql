-- The yearly "still at Capgemini?" nudge: a one-time link per open entry, and a record of when
-- we last asked so nobody is chased twice in the same year.

ALTER TABLE public.employment_entries
    ADD COLUMN last_nudged_at timestamp(6) without time zone;

CREATE SEQUENCE public.employment_confirm_tokens_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.employment_confirm_tokens (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone,
    version integer NOT NULL,
    employment_entry_id bigint NOT NULL,
    token_hash character varying(255) NOT NULL,
    expires_at timestamp(6) without time zone NOT NULL,
    used_at timestamp(6) without time zone
);

ALTER TABLE ONLY public.employment_confirm_tokens
    ADD CONSTRAINT employment_confirm_tokens_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.employment_confirm_tokens
    ADD CONSTRAINT employment_confirm_tokens_token_hash_key UNIQUE (token_hash);

ALTER TABLE ONLY public.employment_confirm_tokens
    ADD CONSTRAINT employment_confirm_tokens_entry_fk FOREIGN KEY (employment_entry_id) REFERENCES public.employment_entries(id) ON DELETE CASCADE;
