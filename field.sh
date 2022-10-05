#!/usr/bin/env bash

# bizzaro trick for getting the directory that this script file is in
main=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
java=$main/jdk/mac/jbrsdk/Contents/Home/bin/java

if [ ! -f $java ]
then
    echo "ERROR: Was expecting to find a working java VM at $java, and I didn't. Perhaps you need to install one"
    exit 1
else
    echo
#  echo Launching Field from $main ...
fi

$java -javaagent:$main/lib/jars/jar-loader.jar  -Dorg.lwjgl.util.Debug=false -Xdock:icon="/Users/marc/Documents/F.png"  -Xdock:name="Field" -classpath "$main/target/classes/:$main/lib/:$main/src/main/java/:$main/target/dependency/*:$main/lib/jars/*" -DappDir=$main/ --add-opens java.desktop/java.awt.event=ALL-UNNAMED -XstartOnFirstThread -Dorg.lwjgl.util.DebugLoader=true -Djava.library.path=/opt/homebrew/lib/python3.9/site-packages/jep/  fieldbox.FieldBox -retina 1 -extraZoomLevel 2.0 -thread2 1 "$@"
