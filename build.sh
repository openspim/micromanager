#!/bin/sh

: "Fiji's Java is located at $FIJI_JAVA_HOME"

cd "$(dirname "$0")" &&
vcexpress.sh MMCoreJ_wrap/MMCoreJ_wrap.sln //build Debug //project mmstudio &&
PATH=$FIJI_JAVA_HOME/bin:$PATH ./bin_Win32/ImageJ-win32.exe --build &&
start mmstudio/Debug/BuildLog.htm &
