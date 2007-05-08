#!/bin/sh --

JAVA=%JAVA%
DEBUG=%DEBUG%
VERSION=%VERSION%
JARPATH=%JARPATH%

exec $JAVA -DPid=$$ -DVersion=$VERSION -jar $JARPATH/gsalliere-$VERSION.jar "$@"
