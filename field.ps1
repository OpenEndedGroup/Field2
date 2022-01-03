$main=$PSScriptRoot

if (-not(Test-Path -Path "$main\jdk\win\jbrsdk\bin\java.exe")) {
    echo "couldn't find Java VM at $main\jdk\win\jbrsdk\bin\java.exe. Likely, you have to download and install one into that directory. See the instructions at $main\jdk\win\readme.md for more help"
    exit
}

&$main\jdk\win\jbrsdk\bin\java.exe -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -classpath ".\target\classes\;.\lib\;.\src\main\java\;.\target\dependency\*;.\lib\jars\*" -DappDir="$main" --add-opens java.desktop/java.awt.event=ALL-UNNAMED fieldbox.FieldBox -retina 0 -extraZoomLevel 1.0 $args
