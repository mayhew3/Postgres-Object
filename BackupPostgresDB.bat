setlocal
For /f "tokens=2-4 delims=/ " %%a in ('date /t') do (set mydate=%%c_%%a_%%b)
For /f "tokens=1-2 delims=/: " %%a in ("%TIME%") do (set mytime=%%a_%%b)
set fulldatetime=%mydate%_%mytime%
set fullBackupPath=D:\Projects\mean_projects\backups_postgres\%fulldatetime%

set PGPASSFILE=%postgres_pgpass_heroku%
echo %PGPASSFILE%

echo %fullBackupPath%
echo Backing up

call pg_dump --host=ec2-54-204-30-115.compute-1.amazonaws.com --dbname=dcg1rtode4vgk9 --username=ijxuheufucvnyu --file=%fullBackupPath%.dump --format=custom

endlocal