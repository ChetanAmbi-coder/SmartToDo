@echo off
title Smart-To-Do-List Desktop App
cd /d "%~dp0"
set "JAVA_HOME=C:\Program Files\Java\jdk-26.0.2.1"
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo Starting Smart-To-Do-List Desktop Application...
call mvn.cmd javafx:run
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Application exited with code %ERRORLEVEL%.
    pause
)
