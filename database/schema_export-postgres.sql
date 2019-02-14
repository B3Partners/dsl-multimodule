--
-- PostgreSQL database dump 
-- NB onbekende versie
--


CREATE TABLE database_inout (
    id bigint NOT NULL,
    db_alias character varying(255),
    col_x character varying(255),
    col_y character varying(255),
    database_name character varying(255),
    host_name character varying(255),
    name character varying(255),
    organization_id integer,
    password character varying(255),
    port integer,
    db_schema character varying(255),
    srs character varying(255),
    buffersize character varying(255),
    timeout character varying(255),
    database_type character varying(255) NOT NULL,
    inout_type character varying(255) NOT NULL,
    url character varying(255),
    user_id integer,
    username character varying(255),
    webservice_db boolean NOT NULL
);


CREATE SEQUENCE hibernate_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


CREATE TABLE input_output (
    id bigint NOT NULL,
    input_output_datatype character varying(255) NOT NULL,
    file_name character varying(255),
    name character varying(255),
    organization_id integer,
    srs character varying(255),
    table_name character varying(255),
    template_output character varying(255),
    input_output_type character varying(255) NOT NULL,
    user_id integer,
    database_id bigint
);


CREATE TABLE mail (
    id bigint NOT NULL,
    from_email_address character varying(255),
    smtp_host character varying(255),
    subject character varying(255),
    to_email_address character varying(255) NOT NULL
);


CREATE TABLE organization (
    id integer NOT NULL,
    name character varying(255) NOT NULL,
    upload_path character varying(255) NOT NULL
);



CREATE TABLE organization_users (
    organization_id integer,
    users_id integer NOT NULL
);


CREATE TABLE output_organization (
    organization_id integer NOT NULL,
    output_id bigint NOT NULL
);



CREATE TABLE post_action (
    id integer NOT NULL,
    class_name character varying(255) NOT NULL,
    label character varying(255) NOT NULL
);


CREATE TABLE post_action_param (
    id integer NOT NULL,
    param character varying(255) NOT NULL,
    value character varying(255) NOT NULL
);



CREATE TABLE process (
    id bigint NOT NULL,
    actions text NOT NULL,
    append_table boolean NOT NULL,
    drop_table boolean NOT NULL,
    features_end integer,
    features_start integer,
    name character varying(255),
    organization_id integer,
    remarks character varying(255),
    user_id integer,
    user_name character varying(255),
    writer_type character varying(255) NOT NULL,
    input_id bigint NOT NULL,
    linked_process bigint,
    mail_id bigint NOT NULL,
    output_id bigint NOT NULL,
    process_status_id bigint NOT NULL,
    schedule bigint,
    filter character varying(255),
    modify_table boolean,
    modify_filter character varying(255),
    modify_geom boolean
);



CREATE TABLE process_status (
    id bigint NOT NULL,
    executing_job_uuid character varying(255),
    message text,
    process_status_type character varying(255) NOT NULL
);


CREATE TABLE qrtz_blob_triggers (
    trigger_name character varying(200) NOT NULL,
    trigger_group character varying(200) NOT NULL,
    blob_data bytea
);



CREATE TABLE qrtz_calendars (
    calendar_name character varying(200) NOT NULL,
    calendar bytea NOT NULL
);



CREATE TABLE qrtz_cron_triggers (
    trigger_name character varying(200) NOT NULL,
    trigger_group character varying(200) NOT NULL,
    cron_expression character varying(120) NOT NULL,
    time_zone_id character varying(80)
);


CREATE TABLE qrtz_fired_triggers (
    entry_id character varying(95) NOT NULL,
    trigger_name character varying(200) NOT NULL,
    trigger_group character varying(200) NOT NULL,
    is_volatile boolean NOT NULL,
    instance_name character varying(200) NOT NULL,
    fired_time bigint NOT NULL,
    priority integer NOT NULL,
    state character varying(16) NOT NULL,
    job_name character varying(200),
    job_group character varying(200),
    is_stateful boolean,
    requests_recovery boolean
);

CREATE TABLE qrtz_job_details (
    job_name character varying(200) NOT NULL,
    job_group character varying(200) NOT NULL,
    description character varying(250),
    job_class_name character varying(250) NOT NULL,
    is_durable boolean NOT NULL,
    is_volatile boolean NOT NULL,
    is_stateful boolean NOT NULL,
    requests_recovery boolean NOT NULL,
    job_data bytea
);



CREATE TABLE qrtz_job_listeners (
    job_name character varying(200) NOT NULL,
    job_group character varying(200) NOT NULL,
    job_listener character varying(200) NOT NULL
);


CREATE TABLE qrtz_locks (
    lock_name character varying(40) NOT NULL
);


CREATE TABLE qrtz_paused_trigger_grps (
    trigger_group character varying(200) NOT NULL
);


CREATE TABLE qrtz_scheduler_state (
    instance_name character varying(200) NOT NULL,
    last_checkin_time bigint NOT NULL,
    checkin_interval bigint NOT NULL
);



CREATE TABLE qrtz_simple_triggers (
    trigger_name character varying(200) NOT NULL,
    trigger_group character varying(200) NOT NULL,
    repeat_count bigint NOT NULL,
    repeat_interval bigint NOT NULL,
    times_triggered bigint NOT NULL
);

CREATE TABLE qrtz_trigger_listeners (
    trigger_name character varying(200) NOT NULL,
    trigger_group character varying(200) NOT NULL,
    trigger_listener character varying(200) NOT NULL
);

CREATE TABLE qrtz_triggers (
    trigger_name character varying(200) NOT NULL,
    trigger_group character varying(200) NOT NULL,
    job_name character varying(200) NOT NULL,
    job_group character varying(200) NOT NULL,
    is_volatile boolean NOT NULL,
    description character varying(250),
    next_fire_time bigint,
    prev_fire_time bigint,
    priority integer,
    trigger_state character varying(16) NOT NULL,
    trigger_type character varying(8) NOT NULL,
    start_time bigint NOT NULL,
    end_time bigint,
    calendar_name character varying(200),
    misfire_instr smallint,
    job_data bytea
);


CREATE TABLE schedule (
    id bigint NOT NULL,
    cron_expression character varying(120) NOT NULL,
    from_date date,
    job_name character varying(120) NOT NULL,
    schedule_type character varying(255) NOT NULL
);

CREATE TABLE users (
    id integer NOT NULL,
    is_admin boolean NOT NULL,
    name character varying(255) NOT NULL,
    password character varying(255) NOT NULL
);


ALTER TABLE ONLY database_inout
    ADD CONSTRAINT database_inout_pkey PRIMARY KEY (id);


ALTER TABLE ONLY input_output
    ADD CONSTRAINT input_output_pkey PRIMARY KEY (id);



ALTER TABLE ONLY mail
    ADD CONSTRAINT mail_pkey PRIMARY KEY (id);


ALTER TABLE ONLY organization
    ADD CONSTRAINT organization_pkey PRIMARY KEY (id);


ALTER TABLE ONLY organization_users
    ADD CONSTRAINT organization_users_pkey PRIMARY KEY (users_id);


ALTER TABLE ONLY post_action_param
    ADD CONSTRAINT post_action_param_pkey PRIMARY KEY (id);

ALTER TABLE ONLY post_action
    ADD CONSTRAINT post_action_pkey PRIMARY KEY (id);



ALTER TABLE ONLY process
    ADD CONSTRAINT process_pkey PRIMARY KEY (id);


ALTER TABLE ONLY process_status
    ADD CONSTRAINT process_status_pkey PRIMARY KEY (id);


ALTER TABLE ONLY qrtz_blob_triggers
    ADD CONSTRAINT qrtz_blob_triggers_pkey PRIMARY KEY (trigger_name, trigger_group);


ALTER TABLE ONLY qrtz_calendars
    ADD CONSTRAINT qrtz_calendars_pkey PRIMARY KEY (calendar_name);



ALTER TABLE ONLY qrtz_cron_triggers
    ADD CONSTRAINT qrtz_cron_triggers_pkey PRIMARY KEY (trigger_name, trigger_group);


ALTER TABLE ONLY qrtz_fired_triggers
    ADD CONSTRAINT qrtz_fired_triggers_pkey PRIMARY KEY (entry_id);


ALTER TABLE ONLY qrtz_job_details
    ADD CONSTRAINT qrtz_job_details_pkey PRIMARY KEY (job_name, job_group);


ALTER TABLE ONLY qrtz_job_listeners
    ADD CONSTRAINT qrtz_job_listeners_pkey PRIMARY KEY (job_name, job_group, job_listener);

ALTER TABLE ONLY qrtz_locks
    ADD CONSTRAINT qrtz_locks_pkey PRIMARY KEY (lock_name);


ALTER TABLE ONLY qrtz_paused_trigger_grps
    ADD CONSTRAINT qrtz_paused_trigger_grps_pkey PRIMARY KEY (trigger_group);


ALTER TABLE ONLY qrtz_scheduler_state
    ADD CONSTRAINT qrtz_scheduler_state_pkey PRIMARY KEY (instance_name);

ALTER TABLE ONLY qrtz_simple_triggers
    ADD CONSTRAINT qrtz_simple_triggers_pkey PRIMARY KEY (trigger_name, trigger_group);

ALTER TABLE ONLY qrtz_trigger_listeners
    ADD CONSTRAINT qrtz_trigger_listeners_pkey PRIMARY KEY (trigger_name, trigger_group, trigger_listener);


ALTER TABLE ONLY qrtz_triggers
    ADD CONSTRAINT qrtz_triggers_pkey PRIMARY KEY (trigger_name, trigger_group);


ALTER TABLE ONLY schedule
    ADD CONSTRAINT schedule_pkey PRIMARY KEY (id);

ALTER TABLE ONLY users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);

CREATE INDEX idx_qrtz_ft_job_group ON qrtz_fired_triggers USING btree (job_group);


CREATE INDEX idx_qrtz_ft_job_name ON qrtz_fired_triggers USING btree (job_name);


CREATE INDEX idx_qrtz_ft_job_req_recovery ON qrtz_fired_triggers USING btree (requests_recovery);


CREATE INDEX idx_qrtz_ft_job_stateful ON qrtz_fired_triggers USING btree (is_stateful);


CREATE INDEX idx_qrtz_ft_trig_group ON qrtz_fired_triggers USING btree (trigger_group);


CREATE INDEX idx_qrtz_ft_trig_inst_name ON qrtz_fired_triggers USING btree (instance_name);


CREATE INDEX idx_qrtz_ft_trig_name ON qrtz_fired_triggers USING btree (trigger_name);

CREATE INDEX idx_qrtz_ft_trig_nm_gp ON qrtz_fired_triggers USING btree (trigger_name, trigger_group);


CREATE INDEX idx_qrtz_ft_trig_volatile ON qrtz_fired_triggers USING btree (is_volatile);


CREATE INDEX idx_qrtz_j_req_recovery ON qrtz_job_details USING btree (requests_recovery);


CREATE INDEX idx_qrtz_t_next_fire_time ON qrtz_triggers USING btree (next_fire_time);


CREATE INDEX idx_qrtz_t_nft_st ON qrtz_triggers USING btree (next_fire_time, trigger_state);


CREATE INDEX idx_qrtz_t_state ON qrtz_triggers USING btree (trigger_state);


CREATE INDEX idx_qrtz_t_volatile ON qrtz_triggers USING btree (is_volatile);


ALTER TABLE ONLY input_output
    ADD CONSTRAINT fk3d134716341b5076 FOREIGN KEY (database_id) REFERENCES database_inout(id);


ALTER TABLE ONLY output_organization
    ADD CONSTRAINT fk7301a7b11ccf9206 FOREIGN KEY (output_id) REFERENCES input_output(id);


ALTER TABLE ONLY output_organization
    ADD CONSTRAINT fk7301a7b1a0c96776 FOREIGN KEY (organization_id) REFERENCES organization(id);

ALTER TABLE ONLY organization_users
    ADD CONSTRAINT fkdabfebfc8bf8659e FOREIGN KEY (users_id) REFERENCES users(id);

ALTER TABLE ONLY organization_users
    ADD CONSTRAINT fkdabfebfca0c96776 FOREIGN KEY (organization_id) REFERENCES organization(id);

ALTER TABLE ONLY process
    ADD CONSTRAINT fked8d1e6f1ccf9206 FOREIGN KEY (output_id) REFERENCES input_output(id);


ALTER TABLE ONLY process
    ADD CONSTRAINT fked8d1e6f3e7ec89c FOREIGN KEY (linked_process) REFERENCES process(id);


ALTER TABLE ONLY process
    ADD CONSTRAINT fked8d1e6f4594b9ca FOREIGN KEY (schedule) REFERENCES schedule(id);

ALTER TABLE ONLY process
    ADD CONSTRAINT fked8d1e6f5b1356dd FOREIGN KEY (process_status_id) REFERENCES process_status(id);


ALTER TABLE ONLY process
    ADD CONSTRAINT fked8d1e6f80dcc4f6 FOREIGN KEY (mail_id) REFERENCES mail(id);

ALTER TABLE ONLY process
    ADD CONSTRAINT fked8d1e6fb72ff29d FOREIGN KEY (input_id) REFERENCES input_output(id);


ALTER TABLE ONLY qrtz_blob_triggers
    ADD CONSTRAINT qrtz_blob_triggers_trigger_name_fkey FOREIGN KEY (trigger_name, trigger_group) REFERENCES qrtz_triggers(trigger_name, trigger_group);

ALTER TABLE ONLY qrtz_cron_triggers
    ADD CONSTRAINT qrtz_cron_triggers_trigger_name_fkey FOREIGN KEY (trigger_name, trigger_group) REFERENCES qrtz_triggers(trigger_name, trigger_group);

ALTER TABLE ONLY qrtz_job_listeners
    ADD CONSTRAINT qrtz_job_listeners_job_name_fkey FOREIGN KEY (job_name, job_group) REFERENCES qrtz_job_details(job_name, job_group);


ALTER TABLE ONLY qrtz_simple_triggers
    ADD CONSTRAINT qrtz_simple_triggers_trigger_name_fkey FOREIGN KEY (trigger_name, trigger_group) REFERENCES qrtz_triggers(trigger_name, trigger_group);


ALTER TABLE ONLY qrtz_trigger_listeners
    ADD CONSTRAINT qrtz_trigger_listeners_trigger_name_fkey FOREIGN KEY (trigger_name, trigger_group) REFERENCES qrtz_triggers(trigger_name, trigger_group);


ALTER TABLE ONLY qrtz_triggers
    ADD CONSTRAINT qrtz_triggers_job_name_fkey FOREIGN KEY (job_name, job_group) REFERENCES qrtz_job_details(job_name, job_group);

-- insert default beheerder // beheerder account, password = beheerder
insert into organization(id, name, upload_path) values (1, 'Beheerders', '/');
insert into users(id, name, password, is_admin) values (1, 'beheerder', '1ZkPjF0ZNpQOXRr0TImwog%3D%3D', true);
insert into organization_users(organization_id, users_id) values (1, 1);

INSERT INTO qrtz_locks values('TRIGGER_ACCESS');
INSERT INTO qrtz_locks values('JOB_ACCESS');
INSERT INTO qrtz_locks values('CALENDAR_ACCESS');
INSERT INTO qrtz_locks values('STATE_ACCESS');
INSERT INTO qrtz_locks values('MISFIRE_ACCESS');
