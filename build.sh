#!/bin/sh

die() {
	echo $1;
	exit 1;
}

banner() {
	echo;
	len=$(echo "$1" | wc -c);
	printf "%*s%s%s%s%*s\n" "$(expr \( 61 - $len \) / 2 )" "" "----==== " "$1" " ====----" "$(expr \( 62 - $len \) / 2 )" "";
	echo;
}

bannerdie() {
	banner "$@";
	exit 1;
}

registry_query() {
	printf '%s\n%s\n' \
		'package require registry 1.0' \
		"puts [registry get \"$1\" \"$2\"]" |
	tclsh
}

platform="Win32"
config="Release"
target=
slnver=

for arg;
do
	case $arg in
	--x64)
		platform="x64";
		;;
	--debug)
		config="Debug";
		;;
	--rebuild)
		target=":REBUILD";
		;;
	--vc100)
		slnver="_v10";
		;;
	--vc90)
		slnver="_v9";
		;;
	*)
		;;
	esac;
done;

banner "FINDING MY MARBLES D:";

dotnetfwdir="$(registry_query "HKEY_LOCAL_MACHINE\\\\SOFTWARE\\\\Microsoft\\\\VisualStudio\\\\SxS\\\\VC7" "FrameworkDir32")"
dotnetfwver="$(registry_query "HKEY_LOCAL_MACHINE\\\\SOFTWARE\\\\Microsoft\\\\VisualStudio\\\\SxS\\\\VC7" "FrameworkVer32")"

test -z "$slnver" && test "$(echo "$dotnetfwver" | head -c2)" = "v4" && slnver="_v10";
test "$slnver" = "_v9" && slnver='' dotnetfwver='v3.5';

msbuild="$dotnetfwdir$dotnetfwver\\msbuild.exe";

test -x $msbuild || die "Couldn't find msbuild.exe ($msbuild). No .NET framework?";

echo "Found msbuild.exe at $msbuild (fw "$dotnetfwver").";

# TODO: Is this recorded anywhere definitive?
ant="$(pwd)/../3rdpartypublic/apache-ant-1.6.5/bin/ant";

test -x $ant || die "Couldn't find Apache Ant at $ant. Did you pull 3rdpartypublic?";

echo "Found ant at $ant.";

banner "${target:+RE}BUILDING MMCORE & MMSTUDIO IN $(echo $config | tr '[:lower:]' '[:upper:]') FOR $(echo $platform | tr '[:lower:]' '[:upper:]')";

# For some reason, you can't specify Build as a target -- you must leave off the target specifier entirely.

$msbuild MMCoreJ_wrap/MMCoreJ_wrap${slnver}.sln /property:Configuration=$config /property:Platform=$platform /target:MMCore${target}\;MMCoreJ_wrap${target}\;mmstudio${target} //fileLogger1 //verbosity:minimal && test "$(grep -c '^Build FAILED\.$' msbuild1.log)" == "0" || bannerdie "FAILED TO BUILD DEVICE ADAPTERS! :(";

banner "${target:+RE}BUILDING DEVICE ADAPTERS IN $(echo $config | tr '[:lower:]' '[:upper:]') FOR $(echo $platform | tr '[:lower:]' '[:upper:]')";

$msbuild MMCoreJ_wrap/MMCoreJ_wrap${slnver}.sln /property:Configuration=$config /property:Platform=$platform /target:DemoCamera${target}\;PicardStage${target}\;SerialManager${target}\;CoherentCube${target} //fileLogger2 //verbosity:minimal && test "$(grep -c '^Build FAILED\.$' msbuild2.log)" == "0" || bannerdie "FAILED TO BUILD DEVICE ADAPTERS! :(";

banner "BUILDING MICRO-MANAGER ACQUISITION ENGINE";

$ant -quiet -buildfile "acqEngine/build.xml" ${target:+clean }compile build || bannerdie "FAILED TO BUILD ACQUISITION ENGINE! :(";

banner "BUILDING MICRO-MANAGER PLUGINS";

$ant -quiet -buildfile "plugins/build.xml" ${target:+clean }compile build || bannerdie "FAILED TO BUILD MICRO-MANAGER PLUGINS! :(";

banner "DONE! :D";
