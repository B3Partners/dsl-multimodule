-- postgres
alter table database_inout ADD column webservice_db boolean;
update database_inout set webservice_db = false;
