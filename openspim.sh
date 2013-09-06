#!/bin/sh

reqistry_query () {
	printf '%s\n%s\n' \
		'package require registry 1.0' \
		"puts [registry get \"$1\" \"\"]" |
	tclsh
}

VCEXPRESS_URL=http://www.microsoft.com/visualstudio/eng/downloads\#d-2010-express

VCEXPRESS="$(reqistry_query "HKEY_LOCAL_MACHINE\\\\SOFTWARE\\\\Microsoft\\\\Windows\\\\CurrentVersion\\\\App Paths\\\\VCExpress.exe")"

SRC=/src
test ! -d /src/fiji/modules/micromanager ||
SRC=/src/fiji/modules
FIJI_JAVA_HOME="$SRC/micromanager/bin_Win32/java/win32/jdk1.6.0_24"

maxwidth () {
	while test $# -gt 0
	do
		printf '%s' "$1" | wc -c
		shift
	done |
	tr -dc '0-9\n' |
	sort -n |
	tail -n 1
}

repeat () {
	counter="$1"
	while test $counter -gt 0
	do
		printf '%s' "$2"
		counter=$(($counter-1))
	done
}

box () {
	indent=5
	indent1=$(($indent - 1))
	width=$(maxwidth "$@")
	printf '\n\n\n% *s%s\n' \
		$indent ' ' $(repeat $(($width + 2 * $indent)) '!')
	while test $# -gt 0
	do
		printf '%-*s!% *s%-*s% *s!\n' \
			$indent ' ' $indent1 ' ' $width "$1" $indent1 ' '
		shift
		test $# = 0 ||
		printf '%-*s!% *s!\n' \
			$indent ' ' $(($width + 2 * $indent - 2)) ' '
	done
	printf '% *s%s\n\n\n' \
		$indent ' ' $(repeat $(($width + 2 * $indent)) '!')
}

if ! test -x "$VCEXPRESS"
then
	box "Please install Visual C++ Express:" \
		"$VCEXPRESS_URL" \
		"This is needed to compile Micro-Manager."
elif test $SRC/micromanager/openspim.sh -nt "$BASH_ARGV"
then
	. $SRC/micromanager/openspim.sh
else

	if ! test -f /bin/git.exe
	then
		(cd /git &&
		 make install)
	fi &&
	git config --global core.autocrlf false &&

	if ! test -x "$HOME/bin/vcexpress.sh"
	then
		mkdir -p "$HOME/bin" &&
		cat > "$HOME/bin/vcexpress.sh" << EOF
#!/bin/sh

export JAVA_HOME="\$(cd "$FIJI_JAVA_HOME" && pwd -W)"
exec "$VCEXPRESS" "\$@"
EOF
	fi &&

	(mkdir -p $SRC &&
	 cd $SRC &&

	 if ! test -d micromanager
	 then
		echo "Cloning Micro-Manager" &&
		git clone git://github.com/openspim/micromanager &&
		(cd micromanager/ &&
		 git config remote.origin.pushURL \
			contrib@fiji.sc:/srv/git/micromanager1.4 &&
		 git config branch.openspim.rebase interactive)
	 fi &&

	 if ! test -x "$HOME/bin/ant"
	 then
		cat > "$HOME/bin/ant" << EOF
#!/bin/sh

export JAVA_HOME="\$(cd "$FIJI_JAVA_HOME" && pwd -W)"
exec "$SRC/3rdpartypublic/apache-ant-1.6.5/bin/ant" "\$@"
EOF
	 fi &&
	 if ! test -x "$HOME/bin/jvisualvm"
	 then
		cat > "$HOME/bin/jvisualvm" << EOF
#!/bin/sh

export JAVA_HOME="\$(cd "$FIJI_JAVA_HOME" && pwd -W)"
exec "\$JAVA_HOME/bin/jvisualvm" "\$@"
EOF
	 fi &&
	 (cd micromanager &&
	  if ! test -f bin_Win32/plugins/MMJ_.jar
	  then
		echo "Building Micro-Manager" &&
		JAVA_HOME=$FIJI_JAVA_HOME ./build.sh
	  fi &&
	  add-desktop-shortcut.sh)) &&

	cat << EOF &&

Welcome to the OpenSPIM development environment!
------------------------------------------------

You can rebuild Micromanager by launching

    ./build.sh

in $SRC/micromanager/. Then start Micro-Manager with

    ./start.sh


EOF
	cd $SRC/micromanager
fi
