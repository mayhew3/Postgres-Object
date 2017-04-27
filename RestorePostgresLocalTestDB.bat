setlocal
set fullBackupPath=D:\Projects\mean_projects\backups_postgres_heroku\2017_04_19_3_45

set PGPASSFILE=%postgres_pgpass_local%
echo %PGPASSFILE%

echo %fullBackupPath%
echo Restoring

call pg_restore --host=localhost --username=postgres --dbname=tv_copy --clean --verbose --format=custom %fullBackupPath%.dump

endlocal