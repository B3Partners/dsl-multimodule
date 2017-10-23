-- PostgreSQL
ALTER TABLE database_inout ADD COLUMN webservice_db boolean;
update database_inout set webservice_db = false;