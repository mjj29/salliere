#!/bin/sh --

JAVA=%JAVA%
VERSION=%VERSION%
JARPATH=%JARPATH%

exec $JAVA -jar $JARPATH/salliere-handicaps-$VERSION.jar "$@"
