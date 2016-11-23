setlocal
set fullBackupPath=D:\Dropbox\Projects\MediaMogul\Backups\TV\2016_11_22_19_01

set PGPASSFILE=%postgres_pgpass_local%
echo %PGPASSFILE%

echo %fullBackupPath%
echo Restoring

call pg_restore --host=localhost --username=postgres --dbname=tv_copy --clean --verbose --format=custom %fullBackupPath%.dump

endlocal