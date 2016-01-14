#!/bin/sh
HOST='ftphost'
USER='user'
PASSWD='pw'
FILE=$1
R_PATH='public_html/fibs_stats'
L_PATH='/mnt/zorg/Documents/marekful/Eclipse_Workspace/MatchDogServer/data/'

ftp -n $HOST <<END_SCRIPT
quote USER $USER
quote PASS $PASSWD
cd $R_PATH
lcd $L_PATH
put $FILE
quit
END_SCRIPT
exit 0