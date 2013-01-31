#!/bin/sh

# For a user script, this would not be good enough:
set -e

die () {
	echo "$*" >&2
	exit 1
}

test ! -d Fiji.app || die "Fiji.app/ exists; will not overwrite"

echo "Getting stable Fiji..."

STABLE_FIJI_URL=http://jenkins.imagej.net/job/Stable-Fiji
curl $STABLE_FIJI_URL/lastSuccessfulBuild/artifact/fiji-win32.tar.gz |
tar xzf -

echo "Adding and updating from the OpenSPIM update site..."

IJ="./Fiji.app/ImageJ-win32.exe --console"
eval $IJ --update add-update-site OpenSPIM http://openspim.org/update/
eval $IJ --update update

echo "Zipping..."

ZIP=OpenSPIM-$(date +%Y%m%d).zip
eval $IJ --full-classpath --main-class=fiji.packaging.Packager \
	--jre --prefix=OpenSPIM.app $ZIP

echo "Cleaning up..."

rm -rf Fiji.app

echo "Successfully built $ZIP"

