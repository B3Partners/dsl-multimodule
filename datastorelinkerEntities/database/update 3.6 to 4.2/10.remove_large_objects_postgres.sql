-- No need for oracle, it was a postgres specific bug
-- First, deploy new version, and call http://<server>:<port>/datastorelinker/ConvertLargeObjects.action. Wait till it's finished (without errors) and run this script in your terminal (not connected to the database).
vacuumlo <database>;
