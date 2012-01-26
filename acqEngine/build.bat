if exist classes rmdir classes /s /q
mkdir classes
xcopy /E /y src classes\
"%JAVA_HOME%\bin\java" -cp ../bin_Win32/plugins/clojure.jar;../bin_Win32/plugins/MMCoreJ.jar;../bin_Win32/plugins/ij.jar;../bin_Win32/plugins/MMJ_.jar;../bin_Win32/plugins/bsh.jar;./src -Dclojure.compile.path=classes clojure.lang.Compile org.micromanager.acq-engine
"%JAVA_HOME%\bin\jar" cf MMAcqEngine.jar -C classes\ .    
copy /Y MMAcqEngine.jar ..\bin_Win32\plugins\
