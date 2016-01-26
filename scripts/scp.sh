#!/bin/sh

FILE=$1
R_PATH='~/public_html/fibs_stats/'
L_PATH='/Users/marekful/git/MatchDogServer/data/'

scp $L_PATH/$FILE dandre.hu:$R_PATH

exit 0