#!/bin/sh

FILE=$1
R_PATH='~/public_html/fibs_stats/'
L_PATH='../data/'

scp $L_PATH/$FILE dandre.hu:$R_PATH

exit 0