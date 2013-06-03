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

platform="Win32"
config="Release"
target=

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
	*)
		;;
	esac;
done;

banner "FINDING MY MARBLES D:";

# TODO: Set all the correct environment variables; check the VS2010 bat?
dotnetfwdir=$(reg query HKLM\\SOFTWARE\\Microsoft\\VisualStudio\\SxS\\VC7 //v FrameworkDir32 | grep 'FrameworkDir32' | sed -r -e 's/\s*FrameworkDir32\s*REG_SZ\s*([^\s]*)/\1/g');
dotnetfwver=$(reg query HKLM\\SOFTWARE\\Microsoft\\VisualStudio\\SxS\\VC7 //v FrameworkVer32 | grep 'FrameworkVer32' | sed -r -e 's/\s*FrameworkVer32\s*REG_SZ\s*([^\s]*)/\1/g');

msbuild="$dotnetfwdir$dotnetfwver\\msbuild.exe";

test -a $msbuild || die "Couldn't find msbuild.exe ($msbuild). No .NET framework?";

echo "Found msbuild.exe at $msbuild.";

# TODO: Is this recorded anywhere definitive?
ant="$(pwd)/../3rdpartypublic/apache-ant-1.6.5/bin/ant";

test -a $ant || die "Couldn't find Apache Ant at $ant. Did you pull 3rdpartypublic?";

echo "Found ant at $ant.";

banner "${target:+RE}BUILDING MMCORE & MMSTUDIO IN $(echo $config | tr '[:lower:]' '[:upper:]') FOR $(echo $platform | tr '[:lower:]' '[:upper:]')";

# For some reason, you can't specify Build as a target -- you must leave off the target specifier entirely.

$msbuild MMCoreJ_wrap/MMCoreJ_wrap_v10.sln /property:Configuration=$config /property:Platform=$platform /target:MMCore${target}\;MMCoreJ_wrap${target}\;mmstudio${target} //fileLogger1 //verbosity:minimal && test "$(grep -c '^Build FAILED\.$' msbuild1.log)" == "0" || bannerdie "FAILED TO BUILD DEVICE ADAPTERS! :(";

banner "${target:+RE}BUILDING DEVICE ADAPTERS IN $(echo $config | tr '[:lower:]' '[:upper:]') FOR $(echo $platform | tr '[:lower:]' '[:upper:]')";

$msbuild MMCoreJ_wrap/MMCoreJ_wrap_v10.sln /property:Configuration=$config /property:Platform=$platform /target:DemoCamera${target}\;PicardStage${target}\;SerialManager${target}\;CoherentCube${target} //fileLogger2 //verbosity:minimal && test "$(grep -c '^Build FAILED\.$' msbuild2.log)" == "0" || bannerdie "FAILED TO BUILD DEVICE ADAPTERS! :(";

banner "BUILDING MICRO-MANAGER ACQUISITION ENGINE";

$ant -quiet -buildfile "acqEngine/build.xml" ${target:+clean }compile build || bannerdie "FAILED TO BUILD ACQUISITION ENGINE! :(";

banner "BUILDING MICRO-MANAGER PLUGINS";

$ant -quiet -buildfile "plugins/build.xml" ${target:+clean }compile build || bannerdie "FAILED TO BUILD MICRO-MANAGER PLUGINS! :(";

banner "DONE! :D";
