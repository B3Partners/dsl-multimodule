-- create tables
create table organization (
	id serial,
	name varchar(255) not null,
	upload_path varchar(255) not null,
	primary key (id)
);

create table users (
	id serial,
	name varchar(255) not null,
	password varchar(255) not null,
	is_admin boolean not null DEFAULT false,
	primary key (id)
);

create table organization_users ( 
	organization_id integer NOT NULL default '0', 
	users_id integer UNIQUE NOT NULL, 
	PRIMARY KEY (organization_id, users_id),
	CONSTRAINT fk_users_id FOREIGN KEY (users_id) REFERENCES users (id), 
	CONSTRAINT fk_organization_id FOREIGN KEY (organization_id) REFERENCES organization (id) 
);

create table output_organization ( 
	output_id integer NOT NULL default '0', 
	organization_id integer NOT NULL, 
	PRIMARY KEY (output_id, organization_id),
	CONSTRAINT fk_output_id FOREIGN KEY (output_id) REFERENCES input_output (id), 
	CONSTRAINT fk_organization_id FOREIGN KEY (organization_id) REFERENCES organization (id) 
);



ALTER TABLE process
  ADD COLUMN organization_id integer;
ALTER TABLE process
  ADD COLUMN user_id integer;
  
ALTER TABLE input_output
  ADD COLUMN organization_id integer;
ALTER TABLE input_output
  ADD COLUMN user_id integer;
ALTER TABLE input_output
  ADD COLUMN template_output character varying(255);
  
ALTER TABLE database_inout
  ADD COLUMN organization_id integer;
ALTER TABLE database_inout
  ADD COLUMN user_id integer;
