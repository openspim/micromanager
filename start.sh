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

export JAVA_HOME="$(cd "$FIJI_JAVA_HOME" && pwd -W)"
export PATH="$FIJI_JAVA_HOME"/bin:$PATH
echo "Current path is $PATH"
exec "$(dirname "$0")"/bin_Win32/ImageJ-win32.exe --run Micro-Manager_Studio "$@" --console
