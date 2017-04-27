setlocal
set fullBackupPath=D:\Projects\mean_projects\backups_postgres_local\2016_04_20_0_43

set PGPASSFILE=%postgres_pgpass_local%
echo %PGPASSFILE%

echo %fullBackupPath%
echo Restoring

call createdb --host=localhost --username=postgres tv_copy

endlocal