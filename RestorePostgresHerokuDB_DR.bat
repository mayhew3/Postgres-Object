setlocal
set fullBackupPath=D:\Dropbox\Projects\MediaMogul\Backups\TV\2016_11_22_19_01

echo %fullBackupPath%
echo Restoring

aws s3 cp %fullBackupPath%.dump s3://mediamogulbackups --grants read=uri=http://acs.amazonaws.com/groups/global/AllUsers

endlocal