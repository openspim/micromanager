MMAcqEngine.jar: $(wildcard $(srcdir)/src/org/micromanager/*.clj)
	test -d classes || mkdir classes
	rm -rf classes/*
	cp -R src/org classes/
	java -cp ../dist/jars/MMCoreJ.jar:../dist/jars/clojure.jar:../dist/jars/ij.jar:../dist/plugins/MMJ_.jar:../dist/jars/bsh.jar:./src -Djava.library.path=. -Djava.awt.headless=true -Dclojure.compile.path=classes clojure.lang.Compile org.micromanager.acq-engine
	jar cvf MMAcqEngine.jar -C classes/ .

all: ../mmstudio MMAcqEngine.jar

clean:
	test -e MMAcqEngine.jar && rm MMAcqEngine.jar || echo "MMAcqEngine.jar not found"
