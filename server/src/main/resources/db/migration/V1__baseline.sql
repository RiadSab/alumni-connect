-- V1 baseline: the schema exactly as Hibernate (ddl-auto: update) had built it when
-- Flyway was introduced, captured via pg_dump --schema-only. Fresh databases are built
-- from this file; the pre-existing dev/prod DB is baselined at V1 and skips it.

--
-- PostgreSQL database dump
--






--
-- Name: candidate_profiles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.candidate_profiles (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone,
    version integer NOT NULL,
    bio character varying(255),
    current_company character varying(255),
    current_job_title character varying(255),
    date_of_birth date,
    experience_years integer,
    field_of_study character varying(255),
    github_url character varying(255),
    graduation_year integer,
    is_student boolean NOT NULL,
    linkedin_url character varying(255),
    portfolio_url character varying(255),
    profile_photo_id character varying(255),
    resume_id character varying(255),
    student_id character varying(255),
    user_id bigint NOT NULL,
    CONSTRAINT candidate_profiles_field_of_study_check CHECK (((field_of_study)::text = ANY ((ARRAY['COMPUTER_SCIENCE'::character varying, 'INFORMATION_TECHNOLOGY'::character varying, 'SOFTWARE_ENGINEERING'::character varying, 'DATA_SCIENCE'::character varying, 'CYBER_SECURITY'::character varying, 'NETWORKING'::character varying, 'ARTIFICIAL_INTELLIGENCE'::character varying, 'MACHINE_LEARNING'::character varying, 'WEB_DEVELOPMENT'::character varying, 'MOBILE_DEVELOPMENT'::character varying, 'CLOUD_COMPUTING'::character varying, 'AEROSPACE_ENGINEERING'::character varying, 'MECHANICAL_ENGINEERING'::character varying, 'CIVIL_ENGINEERING'::character varying, 'ELECTRICAL_ENGINEERING'::character varying, 'CHEMICAL_ENGINEERING'::character varying, 'BIOTECHNOLOGY'::character varying, 'ENVIRONMENTAL_ENGINEERING'::character varying, 'INDUSTRIAL_ENGINEERING'::character varying, 'MATERIALS_SCIENCE'::character varying, 'PHYSICS'::character varying, 'MATHEMATICS'::character varying, 'STATISTICS'::character varying, 'CHEMISTRY'::character varying, 'BIOLOGY'::character varying, 'ECONOMICS'::character varying, 'BUSINESS_ADMINISTRATION'::character varying])::text[])))
);


--
-- Name: candidate_profiles_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.candidate_profiles_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: candidate_skills; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.candidate_skills (
    candidate_profile_id bigint NOT NULL,
    skill character varying(255)
);


--
-- Name: companies; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.companies (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone,
    version integer NOT NULL,
    address character varying(255),
    description character varying(2000),
    email character varying(255) NOT NULL,
    field character varying(255),
    logo_id character varying(255),
    name character varying(255) NOT NULL,
    phone character varying(255),
    size character varying(255),
    status character varying(255) NOT NULL,
    status_change_reason character varying(255),
    video_presentation_id character varying(255),
    website character varying(255),
    CONSTRAINT companies_field_check CHECK (((field)::text = ANY ((ARRAY['COMPUTER_SCIENCE'::character varying, 'INFORMATION_TECHNOLOGY'::character varying, 'SOFTWARE_ENGINEERING'::character varying, 'DATA_SCIENCE'::character varying, 'CYBER_SECURITY'::character varying, 'NETWORKING'::character varying, 'ARTIFICIAL_INTELLIGENCE'::character varying, 'MACHINE_LEARNING'::character varying, 'WEB_DEVELOPMENT'::character varying, 'MOBILE_DEVELOPMENT'::character varying, 'CLOUD_COMPUTING'::character varying, 'AEROSPACE_ENGINEERING'::character varying, 'MECHANICAL_ENGINEERING'::character varying, 'CIVIL_ENGINEERING'::character varying, 'ELECTRICAL_ENGINEERING'::character varying, 'CHEMICAL_ENGINEERING'::character varying, 'BIOTECHNOLOGY'::character varying, 'ENVIRONMENTAL_ENGINEERING'::character varying, 'INDUSTRIAL_ENGINEERING'::character varying, 'MATERIALS_SCIENCE'::character varying, 'PHYSICS'::character varying, 'MATHEMATICS'::character varying, 'STATISTICS'::character varying, 'CHEMISTRY'::character varying, 'BIOLOGY'::character varying, 'ECONOMICS'::character varying, 'BUSINESS_ADMINISTRATION'::character varying])::text[]))),
    CONSTRAINT companies_size_check CHECK (((size)::text = ANY ((ARRAY['STARTUP'::character varying, 'SMALL'::character varying, 'MEDIUM'::character varying, 'LARGE'::character varying, 'MULTINATIONAL'::character varying])::text[]))),
    CONSTRAINT companies_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'ACTIVE'::character varying, 'REJECTED'::character varying, 'SUSPENDED'::character varying, 'PARTNER'::character varying])::text[])))
);


--
-- Name: companies_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.companies_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: company_user_profiles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.company_user_profiles (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone,
    version integer NOT NULL,
    company_role character varying(255) NOT NULL,
    "position" character varying(255),
    company_id bigint NOT NULL,
    user_id bigint NOT NULL,
    CONSTRAINT company_user_profiles_company_role_check CHECK (((company_role)::text = ANY ((ARRAY['OWNER'::character varying, 'RECRUITER'::character varying, 'MEMBER'::character varying])::text[]))),
    CONSTRAINT company_user_profiles_position_check CHECK ((("position")::text = ANY ((ARRAY['HR'::character varying, 'MANAGER'::character varying, 'EMPLOYEE'::character varying, 'INTERN'::character varying, 'OTHER'::character varying])::text[])))
);


--
-- Name: company_user_profiles_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.company_user_profiles_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: job_application_certificates; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.job_application_certificates (
    job_application_id bigint NOT NULL,
    certificates_storage_ids character varying(255)
);


--
-- Name: job_applications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.job_applications (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone,
    version integer NOT NULL,
    application_status character varying(255) NOT NULL,
    company_user_note text,
    cover_letter text,
    priority character varying(255),
    rating integer,
    resume_storage_id character varying(255),
    reviewed_at timestamp(6) without time zone,
    applicant_id bigint NOT NULL,
    job_offer_id bigint NOT NULL,
    reviewed_by_id bigint,
    CONSTRAINT job_applications_application_status_check CHECK (((application_status)::text = ANY ((ARRAY['APPLIED'::character varying, 'UNDER_REVIEW'::character varying, 'SCHEDULED_INTERVIEW'::character varying, 'INTERVIEWED'::character varying, 'REJECTED'::character varying, 'ACCEPTED'::character varying, 'WITHDRAWN'::character varying])::text[]))),
    CONSTRAINT job_applications_priority_check CHECK (((priority)::text = ANY ((ARRAY['LOW'::character varying, 'MEDIUM'::character varying, 'HIGH'::character varying])::text[]))),
    CONSTRAINT job_applications_rating_check CHECK ((rating <= 10))
);


--
-- Name: job_applications_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.job_applications_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: job_offers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.job_offers (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone,
    version integer NOT NULL,
    application_deadline timestamp(6) without time zone,
    city character varying(255),
    contact_email character varying(255),
    current_application_count integer,
    description text,
    employment_type character varying(255),
    experience_years integer,
    is_remote boolean,
    is_urgent boolean,
    max_applications integer,
    max_salary integer,
    min_salary integer,
    status character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    company_id bigint NOT NULL,
    posted_by_id bigint NOT NULL,
    CONSTRAINT job_offers_city_check CHECK (((city)::text = ANY ((ARRAY['CASABLANCA'::character varying, 'RABAT'::character varying, 'MARRAKECH'::character varying, 'TANGER'::character varying, 'AGADIR'::character varying, 'FES'::character varying, 'MEKNES'::character varying, 'OUJDA'::character varying, 'EL_JADIDA'::character varying, 'SAFI'::character varying, 'KHOURIBGA'::character varying, 'BERRECHID'::character varying, 'KENITRA'::character varying, 'SETTAT'::character varying, 'BENGUERIR'::character varying, 'TAZA'::character varying, 'NADOR'::character varying, 'AL_HOCEIMA'::character varying, 'TETOUAN'::character varying, 'LARACHE'::character varying, 'SIDI_KACEM'::character varying, 'SIDI_SLIMANE'::character varying, 'KHENIFRA'::character varying, 'BENI_MELLAL'::character varying])::text[]))),
    CONSTRAINT job_offers_employment_type_check CHECK (((employment_type)::text = ANY ((ARRAY['FULL_TIME'::character varying, 'PART_TIME'::character varying, 'CONTRACT'::character varying, 'INTERN'::character varying, 'FREELANCE'::character varying, 'TEMPORARY'::character varying])::text[]))),
    CONSTRAINT job_offers_status_check CHECK (((status)::text = ANY ((ARRAY['OPEN'::character varying, 'CLOSED'::character varying, 'DRAFT'::character varying, 'EXPIRED'::character varying])::text[])))
);


--
-- Name: job_offers_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.job_offers_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: job_requirements; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.job_requirements (
    job_offer_id bigint NOT NULL,
    requirements character varying(255)
);


--
-- Name: job_skills_required; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.job_skills_required (
    job_offer_id bigint NOT NULL,
    skills_required character varying(255)
);


--
-- Name: password_reset_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.password_reset_tokens (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone,
    version integer NOT NULL,
    attempts integer NOT NULL,
    code_hash character varying(255) NOT NULL,
    expires_at timestamp(6) without time zone NOT NULL,
    used_at timestamp(6) without time zone,
    user_id bigint NOT NULL
);


--
-- Name: password_reset_tokens_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.password_reset_tokens_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: saved_jobs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.saved_jobs (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone,
    version integer NOT NULL,
    candidate_id bigint NOT NULL,
    job_offer_id bigint NOT NULL
);


--
-- Name: saved_jobs_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.saved_jobs_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: stored_files; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.stored_files (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone,
    version integer NOT NULL,
    content_type character varying(255) NOT NULL,
    original_filename character varying(255) NOT NULL,
    size bigint NOT NULL,
    storage_id character varying(255) NOT NULL
);


--
-- Name: stored_files_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.stored_files_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone,
    version integer NOT NULL,
    email character varying(255) NOT NULL,
    email_verified boolean,
    first_name character varying(255) NOT NULL,
    last_name character varying(255) NOT NULL,
    password_hash character varying(255) NOT NULL,
    phone_number character varying(255),
    preferred_language character varying(255),
    status_change_reason character varying(255),
    user_status character varying(255) NOT NULL,
    user_type character varying(255) NOT NULL,
    CONSTRAINT users_user_status_check CHECK (((user_status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying, 'SUSPENDED'::character varying, 'PENDING'::character varying, 'DELETED'::character varying, 'REJECTED'::character varying])::text[]))),
    CONSTRAINT users_user_type_check CHECK (((user_type)::text = ANY ((ARRAY['CANDIDATE'::character varying, 'COMPANY_USER'::character varying, 'ADMINISTRATOR'::character varying])::text[])))
);


--
-- Name: users_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.users_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: candidate_profiles candidate_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.candidate_profiles
    ADD CONSTRAINT candidate_profiles_pkey PRIMARY KEY (id);


--
-- Name: companies companies_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.companies
    ADD CONSTRAINT companies_pkey PRIMARY KEY (id);


--
-- Name: company_user_profiles company_user_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_user_profiles
    ADD CONSTRAINT company_user_profiles_pkey PRIMARY KEY (id);


--
-- Name: job_applications job_applications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.job_applications
    ADD CONSTRAINT job_applications_pkey PRIMARY KEY (id);


--
-- Name: job_offers job_offers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.job_offers
    ADD CONSTRAINT job_offers_pkey PRIMARY KEY (id);


--
-- Name: password_reset_tokens password_reset_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_pkey PRIMARY KEY (id);


--
-- Name: saved_jobs saved_jobs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.saved_jobs
    ADD CONSTRAINT saved_jobs_pkey PRIMARY KEY (id);


--
-- Name: stored_files stored_files_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stored_files
    ADD CONSTRAINT stored_files_pkey PRIMARY KEY (id);


--
-- Name: saved_jobs uk202hgolgk23jd7vldy9tcjpfj; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.saved_jobs
    ADD CONSTRAINT uk202hgolgk23jd7vldy9tcjpfj UNIQUE (candidate_id, job_offer_id);


--
-- Name: users uk6dotkott2kjsp8vw4d0m25fb7; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email);


--
-- Name: stored_files uk7nwc6oid3buy2g2rq8qpcyylf; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stored_files
    ADD CONSTRAINT uk7nwc6oid3buy2g2rq8qpcyylf UNIQUE (storage_id);


--
-- Name: company_user_profiles ukcpgpqvteavsu1mxpq46dj4yvk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_user_profiles
    ADD CONSTRAINT ukcpgpqvteavsu1mxpq46dj4yvk UNIQUE (user_id);


--
-- Name: candidate_profiles ukm7asead9kaplln9cdupjat1cq; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.candidate_profiles
    ADD CONSTRAINT ukm7asead9kaplln9cdupjat1cq UNIQUE (user_id);


--
-- Name: companies ukqjgsqh1oq7xhof2tdte9l7e2b; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.companies
    ADD CONSTRAINT ukqjgsqh1oq7xhof2tdte9l7e2b UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: job_applications fk40oh43m5xeesausn48nv9a3ti; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.job_applications
    ADD CONSTRAINT fk40oh43m5xeesausn48nv9a3ti FOREIGN KEY (job_offer_id) REFERENCES public.job_offers(id);


--
-- Name: job_application_certificates fk54sode51g12q4nr3mxuhk4y82; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.job_application_certificates
    ADD CONSTRAINT fk54sode51g12q4nr3mxuhk4y82 FOREIGN KEY (job_application_id) REFERENCES public.job_applications(id);


--
-- Name: job_applications fk5ok5you182mthgeotgq7mcyw6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.job_applications
    ADD CONSTRAINT fk5ok5you182mthgeotgq7mcyw6 FOREIGN KEY (applicant_id) REFERENCES public.candidate_profiles(id);


--
-- Name: job_applications fk64onth8k6wqv1ecxft1ps3rxr; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.job_applications
    ADD CONSTRAINT fk64onth8k6wqv1ecxft1ps3rxr FOREIGN KEY (reviewed_by_id) REFERENCES public.company_user_profiles(id);


--
-- Name: job_offers fk8ta7i9ubxobstdem21hgs2byb; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.job_offers
    ADD CONSTRAINT fk8ta7i9ubxobstdem21hgs2byb FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: company_user_profiles fkdodde2spg6rsdbyhe4vacld25; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_user_profiles
    ADD CONSTRAINT fkdodde2spg6rsdbyhe4vacld25 FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: job_requirements fkfqrjby86vyi9dns6yktq7mhed; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.job_requirements
    ADD CONSTRAINT fkfqrjby86vyi9dns6yktq7mhed FOREIGN KEY (job_offer_id) REFERENCES public.job_offers(id);


--
-- Name: company_user_profiles fkg6ao940x04d181024rcib90bx; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_user_profiles
    ADD CONSTRAINT fkg6ao940x04d181024rcib90bx FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: job_offers fkikl713xtvu1ctmuvnhb5ammyg; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.job_offers
    ADD CONSTRAINT fkikl713xtvu1ctmuvnhb5ammyg FOREIGN KEY (posted_by_id) REFERENCES public.company_user_profiles(id);


--
-- Name: password_reset_tokens fkk3ndxg5xp6v7wd4gjyusp15gq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT fkk3ndxg5xp6v7wd4gjyusp15gq FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: saved_jobs fkmc28295q9y40kvbpofuparj81; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.saved_jobs
    ADD CONSTRAINT fkmc28295q9y40kvbpofuparj81 FOREIGN KEY (job_offer_id) REFERENCES public.job_offers(id);


--
-- Name: job_skills_required fkmlppy4qo0aap172qc3yy6xt8q; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.job_skills_required
    ADD CONSTRAINT fkmlppy4qo0aap172qc3yy6xt8q FOREIGN KEY (job_offer_id) REFERENCES public.job_offers(id);


--
-- Name: saved_jobs fkn2dstv3bmv4yrvgks9kpvwjsu; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.saved_jobs
    ADD CONSTRAINT fkn2dstv3bmv4yrvgks9kpvwjsu FOREIGN KEY (candidate_id) REFERENCES public.candidate_profiles(id);


--
-- Name: candidate_profiles fkn7b2se0y378uox9e3aw2bjg13; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.candidate_profiles
    ADD CONSTRAINT fkn7b2se0y378uox9e3aw2bjg13 FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: candidate_skills fkth710w3978vqyonpto6hwyper; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.candidate_skills
    ADD CONSTRAINT fkth710w3978vqyonpto6hwyper FOREIGN KEY (candidate_profile_id) REFERENCES public.candidate_profiles(id);


--
-- PostgreSQL database dump complete
--


