#!/bin/bash --

INPUT=$1
SCORES=$2
NAMES=$3

if [ -z "$NAMES" ]; then
   echo "Usage: ecl2salliere <input.csv> <scores.csv> <names.csv>" 
   exit 1
fi

echo -n > $NAMES
cat $INPUT | ( while read i; 
do
   if [ -z "$i" ] || [ "$i" = ",,,,,,," ]; then
      continue
   elif [ "${i:0:1}" = '"' ]; then
      TEAM=${i:1:1}
      echo "Starting team $TEAM" 1>&2
      board=0
      names=1
   elif [ "1" = "$names" ]; then
      c=0
      echo $i | awk -F, '{print "CU'${TEAM}$(( ++c ))',"$1","$2"\nCU'${TEAM}$(( ++c ))',"$3","$4"\nCU'${TEAM}$(( ++c ))',"$5","$6"\nCU'${TEAM}$(( ++c ))',"$7","$8}' >> $NAMES
      names=0
   elif echo $i | grep '^[EWNS,]*$' >/dev/null; then
      dir[1]=`echo $i | cut -d, -f2`
      dir[2]=`echo $i | cut -d, -f4`
      dir[3]=`echo $i | cut -d, -f6`
      dir[4]=`echo $i | cut -d, -f8`
   else
      (( board++ ))
      for (( j = 1; j <= 4; j++ )); do
         if [ "NS" = "${dir[$j]}" ]; then
            echo -n $board,CU$TEAM$j,OPEW,,,,
            echo $i | awk -F, '{print "!"$'$(( $j * 2 - 1 ))'",!"$'$(( $j * 2 ))'}'
         else
            echo -n $board,OPNS,CU$TEAM$j,,,,
            echo $i | awk -F, '{print "!"$'$(( $j * 2 ))'",!"$'$(( $j * 2 - 1 ))'}'
         fi
      done
   fi
done ) > $SCORES

sed -i 's/,!,/,,/g;s/!$//g' $SCORES
