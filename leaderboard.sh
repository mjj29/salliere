#!/bin/sh --

JAVA=%JAVA%
VERSION=%VERSION%
JARPATH=%JARPATH%

exec $JAVA -jar $JARPATH/leaderboard-$VERSION.jar "$@"
