if exist classes rmdir classes /s /q
if exist build rmdir build /s /q
mkdir classes
mkdir build
xcopy /E /y src classes\
"%JAVA_HOME%\bin\java" -cp ../bin_Win32/plugins/clojure.jar;../bin_Win32/plugins/MMCoreJ.jar;../bin_Win32/jars/ij-1*.jar;../bin_Win32/plugins/MMJ_.jar;../bin_Win32/plugins/bsh-2.0b4.jar;./src -Dclojure.compile.path=classes clojure.lang.Compile org.micromanager.acq-engine
xcopy /E /y src build\
xcopy classes\org\micromanager\AcquisitionEngine2010.class build\org\micromanager
"%JAVA_HOME%\bin\jar" cf MMAcqEngine.jar -C build\ .    
copy /Y MMAcqEngine.jar ..\bin_Win32\plugins
