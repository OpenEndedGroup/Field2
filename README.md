

# Field

Field is an open-source software project initiated by OpenEndedGroup, for the creation of their digital artworks.

It is a cross-platform environment for writing code to rapidly and experimentally assemble and explore algorithmic systems.

<center> It is <i>visual</i>, it is <i>code-based</i>, it is <i>hybrid</i>. </center> 

We think it has something to offer a diverse range of programmers, scientists and artists.

Field runs on Windows and MacOS (x64 and Apple Silicon). Documentation and builds can be found [here](http://openendedgroup.com/field).

## Building

Field is written in Java & Kotlin and is built using IntellJ IDEA. We use Gradle to manage most of our dependencies, but we rely on IntellJ's incremental compiler, with no packaging step, to keep our build cycle very fast.

You'll need a JDK, and not just any one, a JBR 17 JDK with JCEF compiled into it, which you can get from: https://github.com/JetBrains/JetBrainsRuntime/releases — be sure to get the right one for your architecture, and, if you are on MacOS, make sure you get the .tar.gz files not the .pkg files.

1. Import the project into IntelliJ.
2. make sure the extra `deps` library is still added to the main module. This contains a handful of hard to object .jars that are needed from outside of the Maven world.
3. make sure we've selected the right 'profile' from the Maven prpfile list for your computer (there should be choices between `macos-aarch64`, `macos-amd64` and `windows-amd64`)
4. build! (and wait for the dependencies to download)
5. finally, and you only need to do this when dependencies change, run the `dependency:copy-dependencies` maven task from the Maven Plugins palette. 

Then you should be able to run Field from the command line. Mac:

```
./field.sh -file helloWorld.field2
```

Documentation and builds can be found [here](http://openendedgroup.com/field).

