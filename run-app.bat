@echo off
title Smart-To-Do-List Desktop App
cd /d "%~dp0"
echo Starting Smart-To-Do-List Desktop Application...
call mvn.cmd javafx:run
pause
