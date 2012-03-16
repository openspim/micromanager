#!/bin/sh


test -f "$USERPROFILE/Desktop/OpenSPIM Dev Env.lnk" || {
	ico="$(cd "$(dirname "$0")" && pwd -W | sed -e 's/\\/\//g' -e 's/$/\/mm-spim.ico/')"
	BIN=/bin
	test -x $BIN/add-shortcut.tcl || {
		BIN=/share/msysGit
		test -x $BIN/add-shortcut.tcl || {
			echo "Skipping shortcut creation" >&2
			exit 0
		}
	}
	tcl=$BIN/tmp.$$.tcl
	sed -e 's/msysGit.lnk/OpenSPIM Dev Env.lnk/' \
		-e "s|\$resDirectory/msysgitlogo.ico|$ico|" \
	< $BIN/add-shortcut.tcl |
	case $BIN in
	/share/msysGit)
		cat
		;;
	*)
		sed -e 's/\[file dirname \(\[file dirname \[pwd\]\]\)\]/\1/' \
			-e 's/exec make//'
		;;
	esac > $tcl
	tclsh $tcl d
	rm $tcl
}
