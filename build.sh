root=`dirname $0`
cd $root


function join() {
    local IFS=$1
    shift
    echo "$*"
}


if [ ! -d kotlinc ]; then
    echo -- downloading kotlinc --
    curl -L https://github.com/JetBrains/kotlin/archive/build-1.1.2-release-105.zip > kotlinc.zip
    echo -- decompressing --
    unzip kotlinc.zip
    echo -- complete --
fi

echo -- using javac from : --
echo `which javac` / `javac -fullversion`

echo -- using kotlinc version : --
echo `./kotlinc/bin/kotlinc -version`


echo -- removing build directory --

rm -r build
mkdir build
cd build
mkdir agent_classes
mkdir linker_classes

echo

echo -- building field agent jar --
find ../agent/src -iname '*.java' > agent_source
echo -- 1/2 - compiling classes --
javac -Xlint:-deprecation -Xlint:-unchecked -classpath "../agent/lib/*"  @agent_source -d agent_classes/
cd agent_classes
cp -r ../../agent/lib/META-INF .
echo -- 2/2 - making jar --
jar cmf ../../agent/lib/META-INF/MANIFEST.MF ../field_agent.jar .
cd ..

echo

echo -- building field linker jar --
find ../linker/src -iname '*.java' > linker_source
echo -- 1/2 - compiling classes --
javac -Xlint:-deprecation -Xlint:-unchecked -classpath "../linker/lib/*"  @linker_source -d linker_classes/
cd linker_classes
cp -r ../../linker/lib/META-INF .
echo -- 2/2 - making jar --
jar cmf ../../linker/lib/META-INF/MANIFEST.MF ../field_linker.jar .
cd ..

echo

echo -- building main classes -- 
mkdir classes
find ../src -iname '*.java' > source
find ../osx/src -iname '*.java' >> source

cp00=$(join ':' ../lib/jars/*.jar)
cp01=$(join ':' ../osx/lib/jars/gumtree/*.jar)
cp1=$(join ':' ../osx/lib/jars/*)


# XDignore.symbol.file suppresses the otherwise unspressable warning about Unsafe, which will be there until there's a replacement for Unsafe
../kotlinc/bin/kotlinc -jdk-home /Library/Java/JavaVirtualMachines/jdk-9.jdk/Contents/Home/ -J--add-opens -Jjava.base/jdk.internal.misc=ALL-UNNAMED -J--add-opens -Jjava.desktop/sun.awt=ALL-UNNAMED -J--add-opens -Jjava.base/java.lang.reflect=ALL-UNNAMED -J--add-opens -Jjava.base/java.lang=ALL-UNNAMED -J--add-opens -Jjava.base/java.util=ALL-UNNAMED -J--add-opens -Jjava.base/java.util.concurrent.atomic=ALL-UNNAMED -J--add-opens -Jjava.desktop/sun.awt=ALL-UNNAMED -J--add-opens -Jjava.base/java.lang.reflect=ALL-UNNAMED -J--add-opens -Jjava.base/java.lang=ALL-UNNAMED -J--add-opens -Jjava.base/java.util=ALL-UNNAMED  -classpath field_agent.jar:field_linker.jar:$cp00:$cp01:$cp1 -d classes ../src

javac -Xlint:-deprecation -Xlint:-unchecked -XDignore.symbol.file -classpath "field_agent.jar:field_linker.jar:../lib/jars/*:../lib/jars/orientdb/*:../osx/lib/jars/*:classes/"  @source -d classes/

echo -- build complete -- 