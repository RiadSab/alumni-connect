-- In-app notifications. type + subject/context rather than a rendered sentence: the app is
-- bilingual, so the text is built client-side from the type and these two values.

CREATE SEQUENCE public.notifications_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.notifications (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone,
    version integer NOT NULL,
    user_id bigint NOT NULL,
    type character varying(255) NOT NULL,
    subject character varying(255),
    context character varying(255),
    link character varying(255),
    read_at timestamp(6) without time zone,
    CONSTRAINT notifications_type_check CHECK (((type)::text = ANY ((ARRAY['APPLICATION_ACCEPTED'::character varying, 'APPLICATION_REJECTED'::character varying, 'INTERVIEW_SCHEDULED'::character varying])::text[])))
);

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_user_fk FOREIGN KEY (user_id) REFERENCES public.users(id);

-- Every read is "my notifications, newest first" or "my unread count".
CREATE INDEX notifications_user_read_idx ON public.notifications USING btree (user_id, read_at);
