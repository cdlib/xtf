:: Batch file to run the XTF textIndexer on Windows

:: set the home directory
@echo off
set XTF_HOME=../%~dp0

:: Make a classpath containing all the jars in XTF_HOME\WEB-INF\lib
setlocal enabledelayedexpansion
set XTFCP=%CLASSPATH%;%XTF_HOME%\WEB-INF\classes
for %%i in ("%XTF_HOME%"\WEB-INF\lib\*.jar) do set XTFCP=!XTFCP!;%%i

:: And fire off the command.
java -cp "%XTFCP%" -Xms150m -Xmx1500m -Dxtf.home="%XTF_HOME%" -DentityExpansionLimit=256000 -enableassertions org.cdlib.xtf.textIndexer.TextIndexer %*
set XTFCP=
set XTF_HOME=
exit
