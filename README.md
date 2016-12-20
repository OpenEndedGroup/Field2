# Field2

Welcome to the Field2 development repository. _This codebase is experimental and in flux; it is not ready for production work._

## Prerequisites for building & running (on Linux)

 * ```git``` (distributed version control) installed via ```sudo apt-get install git```

 * ```java``` itself. Field2 is tracking the almost, but not quite, bleeding edge of JDK 9 development. Ideally, you'll get a b147 JDK. Note: the [latest JDK 9 EA](https://jdk9.java.net/download/), b148 and above, contains a huge breaking change that breaks everything. Just like many other projects, we're working on that. Direct links to b147 for [win_x64](http://download.java.net/java/jdk9/archive/147/binaries/jdk-9-ea+147_windows-x64_bin.exe), [linux_x64](http://download.java.net/java/jdk9/archive/147/binaries/jdk-9-ea+147_linux-x64_bin.tar.gz) and [macOS](http://download.java.net/java/jdk9/archive/147/binaries/jdk-9-ea+147_osx-x64_bin.dmg).

 * Not strictly a prerequisite - ```Intellij IDEA``` (a java development environment) from http://www.jetbrains.com/idea/.

To build, first take a moment to make sure that there's a working `javac` command on your system, and it points to a JDK 9 EA:

```bash
m0:~ marc$ javac -version
javac 9-ea
```

Then, on macOS and Linux, it should simply be a matter of:

```bash
m0:~ marc$ ./build.sh
```

Then, assuming an error-free build:

```bash
./f_mac fieldbox.FieldBox -file helloField.field2
```

on Linux subsitute ```./f_linux``` for ```./f_mac```. And be prepared to edit these scripts to point directly at your JDK

```fieldbox.FieldBox``` is the Java Class that's the main entry-point into Field2. 
 
In case of confusion, search the issues here and email marc (marc@openendedgroup.com); in case of trouble or doubt, file an issue. 

# License

GPLv3 covers the project as a whole, dependencies not included.

