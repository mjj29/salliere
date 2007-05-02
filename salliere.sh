#!/bin/sh --

JAVA=%JAVA%
DEBUG=%DEBUG%
VERSION=%VERSION%
JARPATH=%JARPATH%

exec $JAVA -DPid=$$ -DVersion=$VERSION -cp $JARPATH/debug-$DEBUG.jar:$JARPATH/itext.jar:$JARPATH/csv.jar:$JARPATH/salliere-$VERSION.jar cx.ath.matthew.salliere.Salliere "$@"
