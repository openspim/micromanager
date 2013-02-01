#!/bin/sh

NEW_FILE=OpenSPIM-dev-env-$(date +%Y%m%d).7z
FILE=PortableGit-1.8.0-preview20121022.7z
URL=http://msysgit.googlecode.com/files/$FILE

die () {
	echo "$*" >&2
	exit 1
}

get_offset () {
	result="$(grep -a -b "$1" "$2")"
	count=$(echo "$result" | wc -l)
	test -n "$result" ||
	count=0
	test 1 = $count ||
	die "Got $count matches of '$1' in '$2'"
	echo "$result" |
	cut -d : -f 1
}

cd "$(dirname "$0")" ||
die "Could not switch into the Micro-Manager directory"

TMPDIR=tmp-dev-env
mkdir $TMPDIR ||
die "Could not make the temporary directory (maybe it exists already?)"

test -f $FILE ||
curl -O $URL

(cd $TMPDIR ||
 die "Could not switch to $TMPDIR"

 cp ../$FILE $NEW_FILE

 mkdir -p etc/profile.d &&
 cp ../openspim.sh etc/profile.d/ ||
 die "Could not copy etc/profile.d/openspim.sh"

 (cd /share/msysGit &&
  make) ||
 die "Could not make create-shortcut.exe"

 mkdir -p bin &&
 cp /share/msysGit/{create-shortcut.exe,add-shortcut.tcl} bin/ ||
 die "Could not copy desktop icon making script"

 /share/7-Zip/7za a $NEW_FILE \
	etc/profile.d/openspim.sh bin/{create-shortcut.exe,add-shortcut.tcl} ||
 die "Could not add files"

 start=$(get_offset 'Progress="yes"' $NEW_FILE) || exit
 end=$(get_offset 'OverwriteMode="0"' $NEW_FILE) || exit

 (dd if=$NEW_FILE bs=$start count=1 &&
  cat << EOF &&
rogress="yes"
Title="OpenSPIM development environment"
BeginPrompt="This installs an OpenSPIM development environment (without Microsoft Visual Studio)"
CancelPrompt="Do you want to cancel OpenSPIM installation?"
ExtractDialogText="Please, wait..."
ExtractPathText="Where do you want to install OpenSPIM?"
ExtractTitle="Extracting..."
GUIFlags="8+32+64+256+4096"
GUIMode="1"
InstallPath="C:\OpenSPIM-dev-env"
RunProgram="\"%%T\\Git-bash.bat\""
EOF
  dd if=$NEW_FILE bs=$end skip=1) > ../$NEW_FILE) &&
rm -r $TMPDIR &&
echo "Created $(pwd -W)\\$NEW_FILE"
