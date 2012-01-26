all <- dist/mmplugins/spim.jar dist/jars/MMAcqEngine.jar

BUILDDIR=build/
CLASSPATH=dist/jars/ij.jar:dist/jars/MMCoreJ.jar:dist/plugins/MMJ_.jar
dist/mmplugins/spim.jar <- plugins/SPIMAcquisition/**/*

dist/plugins/MMJ_.jar <- mmstudio/MMJ_.jar

mmstudio/MMJ_.jar[./dist/fiji-win32.exe --ant -f mmstudio/build32.xml] <- mmstudio/src/**/*

dist/jars/MMAcqEngine.jar <- acqEngine/MMAcqEngine.jar

acqEngine/MMAcqEngine.jar[make -C acqEngine] <- acqEngine/**/*.clj
