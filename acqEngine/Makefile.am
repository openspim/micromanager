jar_DATA = MMAcqEngine.jar

.PHONY: MMAcqEngine.jar

MMAcqEngine.jar:
	$(ANT) -Dmm.javacflags="$(JAVACFLAGS)" \
		-Dmm.java.lib.imagej="$(IJJARPATH)" $(ANTFLAGS) jar

clean-local:
	$(ANT) $(ANTFLAGS) clean
