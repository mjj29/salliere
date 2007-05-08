#!/bin/sh --

JAVA=%JAVA%
VERSION=%VERSION%
JARPATH=%JARPATH%

exec $JAVA -jar $JARPATH/gsalliere-$VERSION.jar "$@"
