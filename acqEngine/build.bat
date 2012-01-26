if exist classes rmdir classes /s /q
mkdir classes
xcopy /E /y src classes\
"%JAVA_HOME%\bin\java" -cp ../dist/jars/clojure.jar;../dist/jars/MMCoreJ.jar;../dist/jars/ij.jar;../dist/plugins/MMJ_.jar;../dist/jars/bsh.jar;./src -Dclojure.compile.path=classes clojure.lang.Compile org.micromanager.acq-engine
"%JAVA_HOME%\bin\jar" cf MMAcqEngine.jar -C classes\ .    
copy /Y MMAcqEngine.jar ..\dist\jars\
