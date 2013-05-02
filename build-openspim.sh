#!/bin/sh

die() {
	echo $1;
	exit 1;
}

banner() {
	echo;
	echo "----==== $1 ====----";
	echo;
}

bannerdie() {
	echo;
	echo "----==== $1 ====----";
	echo;
	exit 1;
}

banner "FINDING MY MARBLES D:"

# TODO: Set all the correct environment variables; check the VS2010 bat?
dotnetfwdir=$(reg query HKLM\\SOFTWARE\\Microsoft\\VisualStudio\\SxS\\VC7 //v FrameworkDir32 | grep 'FrameworkDir32' | sed -r -e 's/\s*FrameworkDir32\s*REG_SZ\s*([^\s]*)/\1/g');
dotnetfwver=$(reg query HKLM\\SOFTWARE\\Microsoft\\VisualStudio\\SxS\\VC7 //v FrameworkVer32 | grep 'FrameworkVer32' | sed -r -e 's/\s*FrameworkVer32\s*REG_SZ\s*([^\s]*)/\1/g');

msbuild="$dotnetfwdir$dotnetfwver\\msbuild.exe";

test -a $msbuild || die "Couldn't find msbuild.exe ($msbuild). No .NET framework?";

echo "Found msbuild.exe at $msbuild."

# TODO: Is this recorded anywhere definitive?
ant="$(pwd)/../3rdpartypublic/apache-ant-1.6.5/bin/ant"

test -a $ant || die "Couldn't find Apache Ant at $ant. Did you pull 3rdpartypublic?";

echo "Found ant at $ant."

banner "BUILDING MMCORE & MMSTUDIO"

$msbuild MMCoreJ_wrap/MMCoreJ_wrap_v10.sln /target:MMCore\;MMCoreJ_wrap\;mmstudio //fileLogger1 //verbosity:minimal || bannerdie "FAILED TO BUILD MMCORE/MMSTUDIO! :(";

test "$(grep -c '^Build FAILED\.$' msbuild1.log)" == "0" || bannerdie "FAILED TO BUILD DEVICE ADAPTERS! :(";

banner "BUILDING DEVICE ADAPTERS"

$msbuild MMCoreJ_wrap/MMCoreJ_wrap_v10.sln /target:DemoCamera\;TIScam\;QCam\;PicardStage\;SerialManager\;CoherentCube //fileLogger2 //verbosity:minimal || bannerdie "FAILED TO BUILD DEVICE ADAPTERS! :(";

test "$(grep -c '^Build FAILED\.$' msbuild2.log)" == "0" || bannerdie "FAILED TO BUILD DEVICE ADAPTERS! :(";

banner "BUILDING MICRO-MANAGER PLUGINS"

$ant -quiet -buildfile "plugins/build.xml" clean compile build || bannerdie "FAILED TO BUILD MICRO-MANAGER PLUGINS! :(";

banner "DONE! :D"
