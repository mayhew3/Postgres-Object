setlocal
:Running
echo Processor (Heroku) is running
start javaw -classpath "D:\Projects\mean_projects\GamesDBUtil\out\artifacts\GamesDBUtil_jar\GamesDBUtil.jar" com.mayhew3.mediamogul.tv.TVDBUpdateProcessor Heroku LogToFile
endlocal