setlocal
set fullBackupPath=D:\Projects\mean_projects\backups_postgres_local\2016_04_21_23_39

set PGPASSFILE=%postgres_pgpass_local%
echo %PGPASSFILE%

echo %fullBackupPath%
echo Restoring

call pg_restore --host=localhost --username=postgres --dbname=tv --clean --verbose --format=custom %fullBackupPath%.dump

endlocal