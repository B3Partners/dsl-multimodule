ALTER TABLE database_inout add  buffersize VARCHAR2(255);
ALTER TABLE database_inout add  timeout VARCHAR2(255);

ALTER TABLE process add  filter VARCHAR2(255);
ALTER TABLE process add  modify_table number(1) DEFAULT 0 NOT NULL;
ALTER TABLE process add  modify_filter VARCHAR2(255);
ALTER TABLE process add  modify_geom number(1) DEFAULT 0 NOT NULL;
