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
quiet="-quiet"
verb="//verbosity:minimal"

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
	--verbose)
		quiet="";
		verb="";
		;;
	*)
		;;
	esac;
done;

banner "PREPARING BUILD ENVIRONMENT";

sh prepare-for-build.sh;

if ! [ "${JAVA_HOME}" ];
then
	export JAVA_HOME="$(pwd)/bin_Win32/java/win32/jdk1.6.0_24";

	if ! test -f "$JAVA_HOME/include/jni.h";
	then
		die "Couldn't locate JDK: JAVA_HOME not set and prepare-for-build.sh may have failed...";
	fi;
fi;

banner "FINDING MY MARBLES D:";

dotnetfwdir="$(registry_query "HKEY_LOCAL_MACHINE\\\\SOFTWARE\\\\Microsoft\\\\VisualStudio\\\\SxS\\\\VC7" "FrameworkDir32")"
dotnetfwver="$(registry_query "HKEY_LOCAL_MACHINE\\\\SOFTWARE\\\\Microsoft\\\\VisualStudio\\\\SxS\\\\VC7" "FrameworkVer32")"

msbuild="$dotnetfwdir$dotnetfwver\\msbuild.exe";

test -x $msbuild || die "Couldn't find msbuild.exe ($msbuild). No .NET framework?";

echo "Found msbuild.exe at $msbuild (fw "$dotnetfwver").";

# TODO: Is this recorded anywhere definitive?
ant="$(pwd)/../3rdpartypublic/apache-ant-1.9.2/bin/ant";

test -x $ant || die "Couldn't find Apache Ant at $ant. Did you pull 3rdpartypublic?";

echo "Found ant at $ant.";

if [ "${target}" ];
then
	banner "CLEANING OUTPUT DIRECTORY"

	$ant ${quiet} -Dmm.architecture=${platform} -buildfile "build.xml" clean-all unstage-all || bannerdie "COULDN'T CLEAN OUTPUT DIRECTORY! :(";
fi;

banner "${target:+RE}BUILDING MMCORE IN $(echo $config | tr '[:lower:]' '[:upper:]') FOR $(echo $platform | tr '[:lower:]' '[:upper:]')";

$msbuild micromanager.sln /property:Configuration=$config /property:Platform=$platform /target:MMCore${target} //fileLogger1 ${verb} && test "$(grep -c '^Build FAILED\.$' msbuild1.log)" == "0" || bannerdie "FAILED TO BUILD MMCORE! :(";

banner "${target:+RE}BUILDING MMCOREJ_WRAP (C++) IN $(echo $config | tr '[:lower:]' '[:upper:]') FOR $(echo $platform | tr '[:lower:]' '[:upper:]')";

$msbuild micromanager.sln /property:Configuration=$config /property:Platform=$platform /target:MMCoreJ_wrap${target} //fileLogger1 ${verb} && test "$(grep -c '^Build FAILED\.$' msbuild1.log)" == "0" || bannerdie "FAILED TO BUILD MMCOREJ_WRAP (C++)! :(";

banner "${target:+RE}BUILDING DEVICE ADAPTERS IN $(echo $config | tr '[:lower:]' '[:upper:]') FOR $(echo $platform | tr '[:lower:]' '[:upper:]')";

$msbuild micromanager.sln /property:Configuration=$config /property:Platform=$platform /target:DemoCamera${target}\;PicardStage${target}\;SerialManager${target}\;CoherentCube${target} //fileLogger2 ${verb} && test "$(grep -c '^Build FAILED\.$' msbuild2.log)" == "0" || bannerdie "FAILED TO BUILD DEVICE ADAPTERS! :(";

banner "${target:+RE}BUILDING MMCOREJ_WRAP (JAVA) IN $(echo $config | tr '[:lower:]' '[:upper:]') FOR $(echo $platform | tr '[:lower:]' '[:upper:]')";

$ant ${quiet} -Dmm.architecture=${platform} -buildfile "MMCoreJ_wrap/build.xml" ${target:+clean }jar || bannerdie "FAILED TO BUILD MMCOREJ_WRAP (JAVA)! :(";

banner "${target:+RE}BUILDING MMSTUDIO IN $(echo $config | tr '[:lower:]' '[:upper:]') FOR $(echo $platform | tr '[:lower:]' '[:upper:]')";

$ant ${quiet} -Dmm.architecture=${platform} -buildfile "mmstudio/build.xml" ${target:+clean }jar || bannerdie "FAILED TO BUILD MMSTUDIO! :(";

banner "${target:+RE}BUILDING MICRO-MANAGER ACQUISITION ENGINE";

$ant ${quiet} -Dmm.architecture=${platform} -buildfile "acqEngine/build.xml" ${target:+clean }jar || bannerdie "FAILED TO BUILD ACQUISITION ENGINE! :(";

banner "${target:+RE}BUILDING MICRO-MANAGER PLUGINS";

$ant ${quiet} -Dmm.architecture=${platform} -buildfile "openspim.build.xml" build-plugins || bannerdie "FAILED TO BUILD MICRO-MANAGER PLUGINS! :(";

banner "${target:+RE}BUILDING MICRO-MANAGER AUTO-FOCUS";

$ant ${quiet} -Dmm.architecture=${platform} -buildfile "autofocus/build.xml" ${target:+clean }jar || bannerdie "FAILED TO BUILD MICRO-MANAGER PLUGINS! :(";

banner "STAGING OUTPUT FILES";

$ant ${quiet} -Dmm.architecture=${platform} -buildfile "openspim.build.xml" stage || bannerdie "FAILED TO STAGE OUTPUT FILES! :(";

banner "DONE! :D";
