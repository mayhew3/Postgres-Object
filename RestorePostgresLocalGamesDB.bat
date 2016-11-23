setlocal
set fullBackupPath=D:\Projects\mean_projects\backups_postgres\2016_11_22_3_00

set PGPASSFILE=%postgres_pgpass_local%
echo %PGPASSFILE%

echo %fullBackupPath%
echo Restoring

call pg_restore --host=localhost --username=postgres --dbname=games --clean --verbose --format=custom %fullBackupPath%.dump

endlocal