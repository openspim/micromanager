#!/bin/sh

cd "$(dirname "$0")"
JENKINS_URL=http://jenkins.imagej.net/job/Stable-Fiji
curl $JENKINS_URL/lastSuccessfulBuild/artifact/fiji-nojre.tar.bz2 |
tar --strip-components=1 -xjvf -
