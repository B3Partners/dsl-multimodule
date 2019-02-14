ALTER TABLE database_inout add column buffersize character varying(255);
ALTER TABLE database_inout add column timeout character varying(255);

ALTER TABLE process add column filter character varying(255);
ALTER TABLE process add column modify_table boolean NOT NULL DEFAULT FALSE;
ALTER TABLE process add column modify_filter character varying(255);
ALTER TABLE process add column modify_geom boolean NOT NULL DEFAULT FALSE;
