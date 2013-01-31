#!/bin/sh

# For a user script, this would not be good enough:
set -e

die () {
	echo "$*" >&2
	exit 1
}

test ! -d Fiji.app || die "Fiji.app/ exists; will not overwrite"

echo "Getting stable Fiji..."

STABLE_FIJI_URL=http://jenkins.imagej.net/job/Stable-Fiji
curl $STABLE_FIJI_URL/lastSuccessfulBuild/artifact/fiji-win32.tar.gz |
tar xzf -

echo "Adding and updating from the OpenSPIM update site..."

IJ="./Fiji.app/ImageJ-win32.exe --console"
eval $IJ --update add-update-site OpenSPIM http://openspim.org/update/
eval $IJ --update update

echo "Zipping..."

ZIP=OpenSPIM-$(date +%Y%m%d).zip
cat << EOF |
import javassist.ClassPool;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

pool = ClassPool.getDefault();
clazz = pool.getCtClass("fiji.packaging.Packager");
clazz.getMethod("addFile", "(Ljava/lang/String;Z)Z").instrument(new ExprEditor() {
	public void edit(MethodCall call) {
		if (call.getMethodName().equals("putNextEntry")) {
			call.replace("String path = \$1;"
				+ "if (path.startsWith(\"Fiji.app/\"))"
				+ " path = \"OpenSPIM.app\" + path.substring(8);"
				+ "putNextEntry(path, \$2, \$3);");
		}
	}
});
c = clazz.toClass();
main = c.getMethod("main", new Class[] { String[].class });
main.invoke(null, new Object[] { new String[] { "$ZIP" } });
EOF
eval $IJ --bsh

echo "Cleaning up..."

rm -rf Fiji.app

echo "Successfully built $ZIP"

