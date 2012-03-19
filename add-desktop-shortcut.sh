#!/bin/sh


test -f "$USERPROFILE/Desktop/OpenSPIM Dev Env.lnk" || {
	ico="$(cd "$(dirname "$0")" && pwd -W | sed -e 's/\\/\//g' -e 's/$/\/mm-spim.ico/')"
	tcl=/share/msysGit/tmp.$$.tcl
	sed -e 's/msysGit.lnk/OpenSPIM Dev Env.lnk/' \
		-e "s|\$resDirectory/msysgitlogo.ico|$ico|" \
	< /share/msysGit/add-shortcut.tcl > $tcl
	$tcl d
	rm $tcl
}
