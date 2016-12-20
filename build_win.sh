root=`dirname $0`
cd $root

JDK="/cygdrive/c/Program Files\Java\jdk-9\bin/"

echo -- using javac from : --
which "$JDK"/javac
echo -- which is version : --
"$JDK"/javac -fullversion
echo --



echo -- removing build directory --

rm -r build
mkdir build
cd build
mkdir agent_classes
mkdir linker_classes


echo -- building field agent jar --
find ../agent/src -iname '*.java' > agent_source
echo -- 1/2 - compiling classes --
"$JDK"/javac -Xlint:-deprecation -Xlint:-unchecked -classpath "../agent/lib/*"  @agent_source -d agent_classes/
cd agent_classes
cp -r ../../agent/lib/META-INF .
echo -- 2/2 - making jar --
"$JDK"/jar cmf ../../agent/lib/META-INF/MANIFEST.MF ../field_agent.jar .
cd ..

echo -- building field linker jar --
find ../linker/src -iname '*.java' > linker_source
echo -- 1/2 - compiling classes --
"$JDK"/javac -Xlint:-deprecation -Xlint:-unchecked -classpath "../linker/lib/*"  @linker_source -d linker_classes/
cd linker_classes
cp -r ../../linker/lib/META-INF .
echo -- 2/2 - making jar --
"$JDK"/jar cmf ../../linker/lib/META-INF/MANIFEST.MF ../field_linker.jar .
cd ..



echo -- building main classes -- 
mkdir classes
find ../src -iname '*.java' > source
find ../win/src -iname '*.java' >> source

# XDignore.symbol.file suppresses the otherwise unspressable warning about Unsafe, which will be there until there's a replacement for Unsafe
"$JDK"/javac -Xlint:-deprecation -Xlint:-unchecked -XDignore.symbol.file -classpath "field_agent.jar;field_linker.jar;../lib/jars/*"  @source -d classes/

echo -- build complete -- 