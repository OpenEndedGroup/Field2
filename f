#!/bin/bash

# edit this path here to point to your jdk
java=/usr/lib/jvm/jdk1.9.0/bin/java


pushd $(dirname `which "$0"`) >/dev/null; fieldhome="$PWD"; popd >/dev/null
out=$fieldhome/out/production/

echo $out
echo LD_LIBRARY_PATH=$out/fieldwork2/linux64 $java -DappDir=$fieldhome -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -Xmx2g -Xms2g -cp "$fieldhome/out/artifacts/fieldlinker/fieldlinker.jar:$fieldhome/lib/*:$fieldhome/out/artifacts/fieldagent_jar/fieldagent.jar:$out/fieldwork2/*:$out/fieldwork2/**:$out/fieldwork2/guava-16.0.1.jar:$out/fieldwork2/:$out/fielded/:$out/fielded/*:$out/fieldbox/:$out/fieldbox/*:$out/fieldnashorn/*:$out/fieldnashorn/:$out/fieldcef/*:$out/fieldcef/" -Djava.library.path=$fieldhome:$out/fieldwork2/linux64/::/usr/local/lib/ fieldagent.Trampoline ${*}


LD_LIBRARY_PATH=$out/fieldwork2/linux64 $java -DappDir=$fieldhome -Xdebug  -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005  -Xmx5g -Xms5g -cp "$fieldhome/out/artifacts/fieldlinker/fieldlinker.jar:$fieldhome/lib/*:$fieldhome/out/artifacts/fieldagent_jar/fieldagent.jar:$out/fieldwork2/*:$out/fieldwork2/**:$out/fieldwork2/guava-16.0.1.jar:$out/fieldwork2/:$out/fielded/:$out/fielded/*:$out/fieldbox/:$out/fieldbox/*:$out/fieldnashorn/*:$out/fieldnashorn/:$out/fieldcef/*:$out/fieldcef/" -Djava.library.path=$fieldhome:$out/fieldwork2/linux64/:$out/fieldbox/:/usr/local/lib/ fieldagent.Trampoline ${*}


