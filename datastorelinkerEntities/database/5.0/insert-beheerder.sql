-- insert default beheerder // beheerder account, password = beheerder
insert into organization(id, name, upload_path) values (1, 'Beheerders', '/');
insert into users(id, name, password, is_admin) values (1, 'beheerder', '1ZkPjF0ZNpQOXRr0TImwog%3D%3D', true);
insert into organization_users(organization_id, users_id) values (1, 1);
