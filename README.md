# Field2

Welcome to the Field2 development repository. _This codebase is experimental and in flux; it is not ready for production work._

## Prerequisites for building & running (on Linux)

 * ```git``` (distributed version control) installed via ```sudo apt-get install git```

 * ```java``` itself. Field2 is tracking the bleeding edge of JDK 9 development. Go get and install a JDK-9-EA from [here](https://jdk9.java.net/download/).

 * Not strictly a prerequisite - ```Intellij IDEA``` (a java development environment) from http://www.jetbrains.com/idea/.

To build, first take a moment to make sure that there's a working `javac` command on your system, and it points to a JDK 9 EA:

```bash
m0:~ marc$ javac -fullversion
javac version "9-ea+154"
```
You might have to mess around with your `$PATH` a little.

Then, on macOS and Linux, it should simply be a matter of:

```bash
m0:~ marc$ ./build.sh
```

On Windows / Cygwin (Babun etc.), try `./build_win.sh`.

Then, assuming an error-free build:

```bash
./f_mac fieldbox.FieldBox -file helloField.field2
```

on Linux subsitute ```./f_linux``` for ```./f_mac```, on Windows/Cygwin ```./f_win```. Be prepared to edit these scripts to point directly at your JDK. ```fieldbox.FieldBox``` is the Java Class that's the main entry-point into Field2. 
 
# License

GPLv3 covers the project as a whole, dependencies not included.

