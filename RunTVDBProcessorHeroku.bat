setlocal
:Running
echo Program is running
start javaw -classpath "D:\Projects\mean_projects\GamesDBUtil\out\artifacts\GamesDBUtil_jar\GamesDBUtil.jar" com.mayhew3.gamesutil.tv.TVDBUpdateProcessor Heroku LogToFile
endlocal