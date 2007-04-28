#!/bin/sh --

JAVA=%JAVA%
DEBUG=%DEBUG%
VERSION=%VERSION%
JARPATH=%JARPATH%

exec $JAVA -DPid=$$ -DVersion=$VERSION -cp $JARPATH/debug-$DEBUG.jar:$JARPATH/csv.jar -jar $JARPATH/salliere-$VERSION.jar "$@"
