# Field2

Welcome to the Field2 development repository. _This codebase is experimental and in flux; it is not ready for production work._

## Prerequisites for building & running (on Linux)

 * ```git``` (distributed version control) installed via ```sudo apt-get install git```

 * ```java``` itself. Field2 is tracking the almost, but not quite, bleeding edge of JDK 9 development. Ideally, you'll get a b147 JDK. [https://jdk9.java.net/download/](latest JDK 9 EA). Note b148 contains a huge breaking change that breaks everything. Just like many other projects, we're working on that.

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

## Plugins (e.g Editor, Processing, Clojure)

We now have a skethc of a Plugin API. Field will write an example to ```~/.field/plugins.edn``` on first run. Edit this to extend the classpath, set options and tell Field to add plugins. So, for example, to run the Processing Plugin, I have a file that reads something like this:

```clojure
{:classpath [ "/Users/marc/Downloads/Processing.app/Contents/Java/core/library/core.jar"] } ; adds the core Processing jar to Field and the place where you are building fieldprocessing
{:plugin fieldprocessing.Processing} ; tells Field to initialize the Processing plugin 

```

Edit that path to point to where you are building Field2 and where you have downloaded Processing 2 to (```.../marc/fieldwork2/...``` and ```.../marc/Downloads/Processing.app/...```).

If something goes wrong initializing a plugin Field will continue to launch, but look in the terminal for the stacktrace and error message.

# License

GPLv3 covers the project as a whole, dependencies not included.

