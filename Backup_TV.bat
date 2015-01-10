setlocal
For /f "tokens=2-4 delims=/ " %%a in ('date /t') do (set mydate=%%c_%%a_%%b)
For /f "tokens=1-2 delims=/: " %%a in ("%TIME%") do (set mytime=%%a_%%b)
set fulldatetime=%mydate%_%mytime%
set fullBackupPath=D:\Projects\mean_projects\backups\TV\%fulldatetime%
echo %fullBackupPath%
set mongoshutdown="0"
tasklist /FI "IMAGENAME eq mongod.exe" 2>NUL | find /I /N "mongod.exe">NUL
if "%ERRORLEVEL%"=="0" goto Running
echo MongoDB isn't started yet. Starting.
set mongoshutdown="1"
start "Mongo Games" D:\Projects\mean\mongodb\bin\mongod.exe -dbpath D:\Projects\mean_projects\GameLord\data\db
echo DB started.
:Running
echo Backing up

call mongodump --db tv --out %fullBackupPath%

if %mongoshutdown%=="1" taskkill /im mongod.exe
endlocal