:: Batch file to run the XTF textIndexer on Windows

:: First, let's check to make sure XTF_HOME is set properly
@echo off
if not exist "%XTF_HOME%" goto NEED_HOME

:: Make a classpath containing all the jars in XTF_HOME\WEB-INF\lib
setlocal enabledelayedexpansion
set XTFCP=%CLASSPATH%;%XTF_HOME%\WEB-INF\classes
for %%i in ("%XTF_HOME%"\WEB-INF\lib\*.jar) do set XTFCP=!XTFCP!;%%i

:: And fire off the command.
java -cp "%XTFCP%" -Xms50m -Xmx1000m -Dxtf.home="%XTF_HOME%" -DentityExpansionLimit=128000 -enableassertions org.cdlib.xtf.textIndexer.TextIndexer %*
set XTFCP=
exit /B 0
                                          
:: Print error message and exit
:NEED_HOME
echo ERROR: XTF_HOME environment variable not set properly.
echo You can set it in the "System" Control Panel; look on the Advanced tab,
echo under "Environment Variables".
echo Note that the value should not include quotes.
exit /B 1


                                
