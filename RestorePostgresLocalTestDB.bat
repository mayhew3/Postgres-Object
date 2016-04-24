setlocal
set fullBackupPath=D:\Projects\mean_projects\backups_postgres_local_test\2016_04_23_21_12

set PGPASSFILE=%postgres_pgpass_local%
echo %PGPASSFILE%

echo %fullBackupPath%
echo Restoring

call pg_restore --host=localhost --username=postgres --dbname=tv_copy --clean --verbose --format=custom %fullBackupPath%.dump

endlocal