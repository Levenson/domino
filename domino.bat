@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  DOMINO startup script for Windows
@rem
@rem ##########################################################################

set NOTESBIN=C:\Program Files (x86)\Notes
set JAVA_CMD=C:\Program Files (x86)\Java\jdk1.7.0_45\bin\java.exe

set CLASSPATH=%NOTESBIN%\jvm\lib\ext\Notes.jar
set LD_LIBRARY_PATH=%NOTESBIN%

@rem set LEIN_JVM_OPTS=-Xmx1024m -Xms1024m -XshowSettings:all
@rem -XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode 
set LEIN_JVM_OPTS=-Xbootclasspath/p:"%CLASSPATH%"
set LEIN_JVM_OPTS=%LEIN_JVM_OPTS% -Djava.library.path="%LD_LIBRARY_PATH%" -Dsun.boot.library.path="%LD_LIBRARY_PATH%"
set LEIN_JVM_OPTS=%LEIN_JVM_OPTS% -Duser.home=C:\Users\abralek

if "%1" == "" goto uberjar

echo "=> LEIN_JVM_OPTS = " %LEIN_JVM_OPTS%
echo "=> JAVA_CMD = " %JAVA_CMD%

lein %*

:uberjar
%JAVA_CMD% %LEIN_JVM_OPTS% -jar %CD%\target\domino-0.2.0-standalone.jar
