#!/bin/sh

FIJI_JAVA_HOME= &&
for d in /src/fiji/java/win32/*
do
	if test -z "$FIJI_JAVA_HOME" || test "$d" -nt "$FIJI_JAVA_HOME"
	then
		FIJI_JAVA_HOME="$d"
	fi
done &&
: "Fiji's Java is located at $FIJI_JAVA_HOME"

cd "$(dirname "$0")" &&
vcexpress.sh MMCoreJ_wrap/MMCoreJ_wrap.sln //build Debug //project mmstudio &&
PATH=$FIJI_JAVA_HOME/bin:$PATH ./dist/ImageJ.exe --build &&
start mmstudio/Debug/BuildLog.htm &
