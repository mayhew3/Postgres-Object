@echo off
For /f "tokens=2-4 delims=/ " %%a in ('date /t') do (set mydate=%%c_%%a_%%b)
For /f "tokens=1-2 delims=/:" %%a in ("%TIME%") do (set mytime=%%a_%%b)
set fulldatetime=%mydate%_%mytime%
set fullBackupPath=D:\Projects\mean_projects\backups\TV\%fulldatetime%
echo %fullBackupPath%