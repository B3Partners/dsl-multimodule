
alter table process add (linked_process number(19,0));
    alter table process 
        add constraint FKED8D1E6F3E7EC89C 
        foreign key (linked_process) 
        references process;
