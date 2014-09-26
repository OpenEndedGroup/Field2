# Field2

Welcome to the Field2 development repository. _This codebase is experimental and in flux; it is not ready for production work._

## Prerequisites for building & running (on Linux)

 * ```git``` (distributed version control) installed via ```sudo apt-get install git```

 * ```ant``` (a java build system) installed via ```sudo apt-get install ant```

 * ```Intellij IDEA``` (a java development environment) from http://www.jetbrains.com/idea/ (you can use the community addition until we can figure out an educational license.

 * ```java``` itself. Either get a JDK 8 (not a JRE 8) from your Linux distribution or install one from Oracle. JDK 8u5 should be sufficient (the EA version 8u20 is good for running Field2 but, alas, JavaC crashes with an internal error while building).

To build, either invest the time importing the code-base into Idea (very recommended), or edit fieldwork2.properties in the repository and and build using ```ant -f fieldwork2.xml```

Statically-linked built versions of native dependencies are included in the repository (currently we have a version of GLFW 3.1, a fast jpeg loading library and LWJGL).

To run Field2 we have the following dependencies:

 * ```wmctrl``` (a window manager tool) installed via ```sudo apt-get install wmctrl```

 * ```chrome developer "channel"``` (a version of Chrome with leading-edge features), installed from [here](https://www.google.com/chrome/browser/?platform=linux&extra=devchannel). You'll need to turn "experimental web platform features" and "experimental javascript features" on at the chrome://flags configuration page.

Then, assuming an error-free build:

```bash
cd ((therepository))
./f fieldbox.FieldBox
```

on OSX subsitute ```./f_mac``` for ```./f```. Note you may need to edit your ```./f``` script to point to the location of your JDK.

```fieldbox.FieldBox``` is the Java Class that's the main entry-point into Field2. 
 
In case of confusion, search the issues here and email marc (marc@openendedgroup.com); in case of trouble or doubt, file an issue. Finally, I'll take everybody through this (on Linux and OS X at least) during a hands-on tutorial.  

## Plugins (e.g Processing)

We've just checked in a Plugin API. Field will write an example to ```~/.field/plugins.edn``` on first run. Edit this to extend the classpath, set options and tell Field to add plugins. So, for example, to run the Processing Plugin, I have a file that reads something like this:

```clojure
{:classpath ["/Users/marc/fieldwork2/out/production/fieldprocessing/" "/Users/marc/Downloads/Processing.app/Contents/Java/core/library/core.jar"] } ; adds the core Processing jar to Field and the place where you are building fieldprocessing
{:plugin fieldprocessing.Processing} ; tells Field to initialize the Processing plugin 
```

Edit the _two paths_ to point to where you are building Field2 and where you have downloaded Processing 2 to (```.../marc/fieldwork2/...``` and ```.../marc/Downloads/Processing.app/...```).

If something goes wrong initializing a plugin Field will continue to launch, but look in the terminal for the stacktrace and error message.

# License

GPLv3 covers the project as a whole, dependencies not included.

