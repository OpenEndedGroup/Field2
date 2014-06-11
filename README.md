# Field2

Welcome to the Field2 development repository.

## Prerequisites for building & running (on Linux)

```git``` (distributed version control) installed via ```sudo apt-get install git```

```ant``` (a java build system) installed via ```sudo apt-get install ant```

```Intellij IDEA``` (a java development environment) from http://www.jetbrains.com/idea/ (you can use the community addition until we can figure out an educational license.

```java``` itself. Either get a JDK 8 (not a JRE 8) from your Linux distribution or install one from Oracle.

To build, either invest the time importing the code-base into Idea (very recommended), or edit fieldwork2.properties in the repository and and build using ```ant -f fieldwork2.xml```

Statically-linked built versions of native dependencies are included in the repository (currently we have a version of GLFW 3.1, a fast jpeg loading library and LWJGL).

To run Field2 we have the following dependencies:

```wmctrl``` (a window manager tool) installed via ```sudo apt-get install wmctrl```
```chrome developer "channel"``` (a version of Chrome with leading-edge features), installed from [here](https://www.google.com/chrome/browser/?platform=linux&extra=devchannel). You'll need to turn "experimental web platform features" and "experimental javascript features" on at the chrome://flags configuration page.

Then, assuming an error-free build:

```bash
cd ((therepository))
./f fieldbox.FieldBox
```

```fieldbox.FieldBox``` is the Java Class that's the main entry-point into Field2. 
 
In case of confusion, email marc (marc@openendedgroup.com); in case of trouble or doubt, file an issue. Finally, I'll take everybody through this (on Linux and OS X at least) during a hands-on tutorial.  

# License

GPLv3 covers the project as a whole, dependencies not included.

