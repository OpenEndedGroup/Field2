SET fieldhome=%~dp0
SET JDK=%fieldhome%/jdk/win/jdk-10/bin/
SET out=%fieldhome%/build/classes

echo %fieldhome%
echo %JDK%

SET FIELD2_LAUNCH=%fieldhome%\f.bat

%JDK%/java.exe -DappDir=%fieldhome% -Xmx8g -Xms4g -javaagent:%fieldhome%/build/field_agent.jar -cp "%fieldhome%/build/classes;%fieldhome%/build/field_linker.jar;%fieldhome%/lib/;%fieldhome%/lib/jars/*;%fieldhome%/build/field_agent.jar" -Djava.library.path=%fieldhome%/win/lib fieldagent.Trampoline %*
