-- Oracle
alter table database_inout ADD (webservice_db NUMBER(1,0));
update database_inout set webservice_db = 0;