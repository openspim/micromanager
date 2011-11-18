all <- dist/mmplugins/spim.jar

BUILDDIR=build/
CLASSPATH=dist/jars/ij.jar:dist/jars/MMCoreJ.jar:dist/plugins/MMJ_.jar
dist/mmplugins/spim.jar <- plugins/SPIMAcquisition/**/*

dist/plugins/MMJ_.jar <- mmstudio/MMJ_.jar

mmstudio/MMJ_.jar[fiji --ant -f mmstudio/build32.xml] <- mmstudio/src/**/*
