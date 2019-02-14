# DSL datamodel

De qtz_*.sql scripts hebben de benodigde tabellen ed. voor de taakplanner, 
deze komen uit de Quartz download: http://www.quartz-scheduler.org/downloads/

Let op dat bij een library upgrade ook deze scripts worden bijgewerkt.

De *-schema-export.sql bevat de met hibernate export gegenereerde tabellen ed.
Deze worden tijdens de maven build gemaakt de schema export plugin die 
de persistence.xml uit het datastolinker war project gebruikt.
De gemaakt sql staat dan in de target/schema-export directory.

de volgorde van uitvoeren van de scripts:

  - schema-export-<DB>.sql
  - qtz_tables_<DB>.sql
  - insert-beheerder.sql

