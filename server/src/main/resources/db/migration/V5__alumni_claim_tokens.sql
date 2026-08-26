-- One-time links that turn a roster row into a real account. Only the SHA-256 hash is stored,
-- so a leaked database row can't be used to claim anyone's account.

CREATE SEQUENCE public.alumni_claim_tokens_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.alumni_claim_tokens (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone,
    version integer NOT NULL,
    alumni_record_id bigint NOT NULL,
    token_hash character varying(255) NOT NULL,
    expires_at timestamp(6) without time zone NOT NULL,
    used_at timestamp(6) without time zone
);

ALTER TABLE ONLY public.alumni_claim_tokens
    ADD CONSTRAINT alumni_claim_tokens_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.alumni_claim_tokens
    ADD CONSTRAINT alumni_claim_tokens_token_hash_key UNIQUE (token_hash);

ALTER TABLE ONLY public.alumni_claim_tokens
    ADD CONSTRAINT alumni_claim_tokens_record_fk FOREIGN KEY (alumni_record_id) REFERENCES public.alumni_records(id);
