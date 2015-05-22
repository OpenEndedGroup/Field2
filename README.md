# Field2

Welcome to the Field2 development repository. _This codebase is experimental and in flux; it is not ready for production work._

## Prerequisites for building & running (on Linux)

 * ```git``` (distributed version control) installed via ```sudo apt-get install git```

 * ```ant``` (a java build system) installed via ```sudo apt-get install ant```

 * Not strictly a prerequisite - ```Intellij IDEA``` (a java development environment) from http://www.jetbrains.com/idea/.

 * ```java``` itself. Either get a JDK 8 or 9 EA. 

 * ```jzmq``` from https://github.com/zeromq/jzmq (this is for IPython support)

To build, either invest the time importing the code-base into Idea (very recommended), or edit ```fieldwork2.properties``` in the repository and and build using ```build.sh``` (this calls ```ant -f fieldwork2.xml```)

Statically-linked built versions of native dependencies are included in the repository (currently we have a version of GLFW 3.1, a fast jpeg loading library and LWJGL).

Then, assuming an error-free build:

```bash
cd ((therepository))
./f fieldbox.FieldBox -file something.field2 -threaded 1 
```

on OSX subsitute ```./f_mac``` for ```./f```. Note you may need to edit your ```./f``` script to point to the location of your JDK. Additionally you'll want to add `-retina 1` to your command line on Retina displays (issue #39) .

```fieldbox.FieldBox``` is the Java Class that's the main entry-point into Field2. 
 
In case of confusion, search the issues here and email marc (marc@openendedgroup.com); in case of trouble or doubt, file an issue. Finally, I'll take everybody through this (on Linux and OS X at least) during a hands-on tutorial.  

## Plugins (e.g Editor, Processing, Clojure)

We've just checked in a Plugin API. Field will write an example to ```~/.field/plugins.edn``` on first run. Edit this to extend the classpath, set options and tell Field to add plugins. So, for example, to run the Processing Plugin, I have a file that reads something like this:

```clojure
; {:classpath [ "/Users/marc/Downloads/Processing.app/Contents/Java/core/library/core.jar"] } ; adds the core Processing jar to Field and the place where you are building fieldprocessing
; {:plugin fieldprocessing.Processing} ; tells Field to initialize the Processing plugin 

{:plugin fieldcef.plugins.GlassBrowser}
{:plugin fieldcef.plugins.TextEditor}
{:plugin fieldcef.plugins.OutputBox}
```

Edit that path to point to where you are building Field2 and where you have downloaded Processing 2 to (```.../marc/fieldwork2/...``` and ```.../marc/Downloads/Processing.app/...```).

Those last three lines (that include plugins from the `fieldcef` module) are optional, but greatly increase the functionality of Field. We'll be making them core shortly.

If something goes wrong initializing a plugin Field will continue to launch, but look in the terminal for the stacktrace and error message.

# License

GPLv3 covers the project as a whole, dependencies not included.

