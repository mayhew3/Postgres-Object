setlocal
:Running
echo Finder (Heroku) is running
start javaw -classpath "D:\Projects\mean_projects\GamesDBUtil\build\libs\GamesDBUtil.jar" com.mayhew3.mediamogul.tv.TVDBUpdateFinder Heroku LogToFile
endlocal