setlocal
echo Program is running
java -classpath "D:\Projects\mean_projects\GamesDBUtil\out\artifacts\GamesDBUtil_jar\GamesDBUtil.jar" com.mayhew3.gamesutil.tv.MetacriticTVUpdater Quick Local
java -classpath "D:\Projects\mean_projects\GamesDBUtil\out\artifacts\GamesDBUtil_jar\GamesDBUtil.jar" com.mayhew3.gamesutil.tv.TiVoLibraryUpdater LogToFile Local
endlocal