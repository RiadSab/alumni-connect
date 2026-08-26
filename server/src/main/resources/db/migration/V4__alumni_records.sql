-- The school's own list of graduates: the denominator every employment figure divides by.
-- A row exists whether or not the person ever creates an account. See docs/alumni-roster.md.

CREATE SEQUENCE public.alumni_records_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.alumni_records (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone,
    version integer NOT NULL,
    student_id character varying(255) NOT NULL,
    first_name character varying(255) NOT NULL,
    last_name character varying(255) NOT NULL,
    field_of_study character varying(255) NOT NULL,
    promotion_year integer NOT NULL,
    email character varying(255),
    claimed_by_user_id bigint,
    claimed_at timestamp(6) without time zone,
    opted_out_at timestamp(6) without time zone,
    CONSTRAINT alumni_records_field_of_study_check CHECK (((field_of_study)::text = ANY ((ARRAY['COMPUTER_SCIENCE'::character varying, 'INFORMATION_TECHNOLOGY'::character varying, 'SOFTWARE_ENGINEERING'::character varying, 'DATA_SCIENCE'::character varying, 'CYBER_SECURITY'::character varying, 'NETWORKING'::character varying, 'ARTIFICIAL_INTELLIGENCE'::character varying, 'MACHINE_LEARNING'::character varying, 'WEB_DEVELOPMENT'::character varying, 'MOBILE_DEVELOPMENT'::character varying, 'CLOUD_COMPUTING'::character varying, 'AEROSPACE_ENGINEERING'::character varying, 'MECHANICAL_ENGINEERING'::character varying, 'CIVIL_ENGINEERING'::character varying, 'ELECTRICAL_ENGINEERING'::character varying, 'CHEMICAL_ENGINEERING'::character varying, 'BIOTECHNOLOGY'::character varying, 'ENVIRONMENTAL_ENGINEERING'::character varying, 'INDUSTRIAL_ENGINEERING'::character varying, 'MATERIALS_SCIENCE'::character varying, 'PHYSICS'::character varying, 'MATHEMATICS'::character varying, 'STATISTICS'::character varying, 'CHEMISTRY'::character varying, 'BIOLOGY'::character varying, 'ECONOMICS'::character varying, 'BUSINESS_ADMINISTRATION'::character varying])::text[])))
);

ALTER TABLE ONLY public.alumni_records
    ADD CONSTRAINT alumni_records_pkey PRIMARY KEY (id);

-- The school's student ID is the natural key: re-importing a corrected file updates rows
-- instead of duplicating them.
ALTER TABLE ONLY public.alumni_records
    ADD CONSTRAINT alumni_records_student_id_key UNIQUE (student_id);

ALTER TABLE ONLY public.alumni_records
    ADD CONSTRAINT alumni_records_claimed_by_user_id_key UNIQUE (claimed_by_user_id);

ALTER TABLE ONLY public.alumni_records
    ADD CONSTRAINT alumni_records_claimed_by_user_fk FOREIGN KEY (claimed_by_user_id) REFERENCES public.users(id);

CREATE INDEX alumni_records_promotion_year_idx ON public.alumni_records USING btree (promotion_year);
