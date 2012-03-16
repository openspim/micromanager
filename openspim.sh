#!/bin/sh

VCEXPRESS="$PROGRAMFILES/Microsoft Visual Studio 9.0/Common7/IDE/VCExpress.exe"

if ! test -x "$VCEXPRESS"
then

	cat >&2 << EOF



      !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
      !       Please install Visual Express Studio:                     !
      !                                                                 !
      !       http://msdn.microsoft.com/en-us/express/future/bb421473   !
      !                                                                 !
      !       This is needed to compile Micro-Manager.                  !
      !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!



EOF

elif test /src/fiji/modules/micromanager/openspim.sh -nt "$BASH_ARGV"
then
	. /src/fiji/modules/micromanager/openspim.sh
else

	if ! test -f /bin/git.exe
	then
		(cd /git &&
		 make install)
	fi &&
	git config --global core.autocrlf false &&

	(cd /src &&

	 if ! test -d fiji
	 then
		echo "Cloning Fiji" &&
		git clone git://fiji.sc/fiji.git &&
		(cd fiji &&
		 git config remote.origin.pushURL \
			contrib@fiji.sc:/srv/git/fiji.git) &&
		mkdir -p /.git/info &&
		echo /src/fiji/ >> /.git/info/exclude
	 fi &&
	 cd fiji/ &&
	 if ! test -x ImageJ.exe
	 then
		echo "Building Fiji" &&
		./Build.sh
	 fi &&

	 FIJI_JAVA_HOME= &&
	 for d in /src/fiji/java/win32/*
	 do
		if test -z "$FIJI_JAVA_HOME" || test "$d" -nt "$FIJI_JAVA_HOME"
		then
			FIJI_JAVA_HOME="$d"
		fi
	 done &&
	 : "Fiji's Java is located at $FIJI_JAVA_HOME" &&

	 if ! test -x "$HOME/bin/vcexpress.sh"
	 then
		mkdir -p "$HOME/bin" &&
		cat > "$HOME/bin/vcexpress.sh" << EOF
#!/bin/sh

export JAVA_HOME="\$(cd "$FIJI_JAVA_HOME" && pwd -W)"
exec "$VCEXPRESS" "\$@"
EOF
	 fi &&

	 cd modules/ &&

	 if ! test -d 3rdpartypublic
	 then
		echo "Cloning Micro-Manager's 3rdparty libraries" &&
		git clone git://fiji.sc/mmanager-3rdparty 3rdpartypublic &&
		(cd 3rdpartypublic/ &&
		 git config remote.origin.pushURL \
			contrib@fiji.sc:/srv/git/mmanager-3rdparty &&
		 git checkout openspim)
	 fi &&
	 if ! test -d 3rdparty
	 then
		echo "Cloning Micro-Manager's 3rdparty libraries" &&
		git clone contrib@fiji.sc:mmanager-private 3rdparty
	 fi &&
	 if ! test -d micromanager
	 then
		echo "Cloning Micro-Manager" &&
		git clone git://fiji.sc/micromanager1.4 micromanager &&
		(cd micromanager/ &&
		 git checkout openspim &&
		 git config remote.origin.pushURL \
			contrib@fiji.sc:/srv/git/micromanager1.4 &&
		 git config branch.openspim.rebase interactive)
	 fi &&
	 (cd micromanager &&
	  if ! test -f bin_Win32/ImageJ.exe
	  then
		echo "Copying Fiji into Micro-Manager's bin_Win32/ directory" &&
		(cd /src/fiji/ &&
		 ./ImageJ.exe --full-classpath \
			--main-class=fiji.packaging.Packager fiji.tar) &&
		(cd bin_Win32/ &&
		 tar --strip-components=1 -xf /src/fiji/fiji.tar)
	  fi &&

	  if ! test -f bin_Win32/plugins/MMJ_.jar
	  then
		echo "Building Micro-Manager" &&
		"$HOME/bin/vcexpress.sh" MMCoreJ_wrap/MMCoreJ_wrap.sln \
			//build Debug \
			//project mmstudio
		start mmstudio/Debug/BuildLog.htm
	  fi &&
	  if ! test -f bin_Win32/jars/MMAcqEngine.jar
	  then
		echo "Building Micro-Manager's acquisition engine" &&
		PATH=$FIJI_JAVA_HOME/bin:$PATH \
		./bin_Win32/ImageJ.exe --build
	  fi)) &&

	cat << EOF &&

Welcome to the OpenSPIM development environment!
------------------------------------------------

You can build Micromanager by launching

    vcexpress.sh /src/fiji/modules/micromanager/MMCoreJ_wrap/MMCoreJ_wrap.sln

building everything by pressing <F7>, and then calling

    ./bin_Win32/ImageJ --build

in /src/fiji/modules/micromanager/. Then start Micro-Manager with

    ./start-openspim.sh


EOF

	cd /src/fiji/modules/micromanager/ &&
	add-desktop-shortcut.sh

fi
