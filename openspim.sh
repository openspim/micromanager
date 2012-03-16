#!/bin/sh

VCEXPRESS="$PROGRAMFILES/Microsoft Visual Studio 9.0/Common7/IDE/VCExpress.exe"
VCEXPRESS_URL=http://msdn.microsoft.com/en-us/express/future/bb421473
STABLE_FIJI_URL=http://jenkins.imagej.net/job/Stable-Fiji
FIJI_URL=$STABLE_FIJI_URL/lastSuccessfulBuild/artifact/fiji-win32.tar.gz
JDK_URL="http://fiji.sc/cgi-bin/gitweb.cgi?p=java/win32.git;a=snapshot;h=HEAD;sf=tgz"

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
	box "Please install Visual Express Studio:" \
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

	 if ! test -d 3rdpartypublic
	 then
		echo "Cloning Micro-Manager's 3rdparty libraries" &&
		git clone git://github.com/openspim/3rdpartypublic &&
		(cd 3rdpartypublic/ &&
		 git config remote.origin.pushURL \
			contrib@fiji.sc:/srv/git/mmanager-3rdparty)
	 fi &&
	 if ! test -d micromanager
	 then
		echo "Cloning Micro-Manager" &&
		git clone git://github.com/openspim/micromanager &&
		(cd micromanager/ &&
		 git config remote.origin.pushURL \
			contrib@fiji.sc:/srv/git/micromanager1.4 &&
		 git config branch.openspim.rebase interactive)
	 fi &&

	 if ! test -f $FIJI_JAVA_HOME/include/jni.h
	 then
		echo "Downloading and unpacking the JDK" &&
		curl "$JDK_URL" |
		(mkdir -p $FIJI_JAVA_HOME &&
		 cd $FIJI_JAVA_HOME &&
		 tar --strip-components=2 -xzf -)
	 fi

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
	  if ! test -f bin_Win32/ImageJ-win32.exe
	  then
		echo "Copying Fiji into Micro-Manager's bin_Win32/ directory" &&
		curl $FIJI_URL |
		(cd bin_Win32/ &&
		 tar --strip-components=1 -xzf - &&
		 ./ImageJ-win32.exe --update add-update-site \
			OpenSPIM http://openspim.org/update/ \
			spim@openspim.org update/)
	  fi &&

	  if ! test -f bin_Win32/plugins/MMJ_.jar
	  then
		echo "Building Micro-Manager" &&
		./build.sh
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
