#!/bin/sh

# TODO: Set all the correct environment variables; check the VS2010 bat.
dotnetfwdir=$(reg query HKLM\\SOFTWARE\\Microsoft\\VisualStudio\\SxS\\VC7 //v FrameworkDir32 | grep 'FrameworkDir32' | sed -r -e 's/\s*FrameworkDir32\s*REG_SZ\s*([^\s]*)/\1/g');
dotnetfwver=$(reg query HKLM\\SOFTWARE\\Microsoft\\VisualStudio\\SxS\\VC7 //v FrameworkVer32 | grep 'FrameworkVer32' | sed -r -e 's/\s*FrameworkVer32\s*REG_SZ\s*([^\s]*)/\1/g');

msbuild="$dotnetfwdir$dotnetfwver\\msbuild.exe";

if ! test -a $msbuild;
then
	echo "Couldn't find msbuild.exe in $msbuild. No .NET framework?";
	exit 1;
fi &&
$msbuild MMCoreJ_wrap/MMCoreJ_wrap_v10.sln /target:OpenSPIM\;CoherentCube //fileLogger //detailedsummary

