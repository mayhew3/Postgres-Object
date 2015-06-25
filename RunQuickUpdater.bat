setlocal
set mongoshutdown="0"
tasklist /FI "IMAGENAME eq mongod.exe" 2>NUL | find /I /N "mongod.exe">NUL
if "%ERRORLEVEL%"=="0" goto Running
echo MongoDB isn't started yet. Starting.
set mongoshutdown="1"
start "Mongo Games" D:\Projects\mean\mongodb\bin\mongod.exe -dbpath D:\Projects\mean_projects\GameLord\data\db
echo DB started.
:Running
echo Program is running
java -classpath "D:\Projects\mean_projects\GamesDBUtil\out\artifacts\GamesDBUtil_jar\GamesDBUtil.jar" com.mayhew3.gamesutil.games.SteamGameUpdater
java -classpath "D:\Projects\mean_projects\GamesDBUtil\out\artifacts\GamesDBUtil_jar\GamesDBUtil.jar" com.mayhew3.gamesutil.TiVoLibraryUpdater TiVoOnly LogToFile
if %mongoshutdown%=="1" taskkill /im mongod.exe
endlocal