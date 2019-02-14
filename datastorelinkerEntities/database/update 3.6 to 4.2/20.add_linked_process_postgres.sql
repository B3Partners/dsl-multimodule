
alter table process add column  linked_process int8;
    alter table process 
        add constraint FKED8D1E6F3E7EC89C 
        foreign key (linked_process) 
        references process;
