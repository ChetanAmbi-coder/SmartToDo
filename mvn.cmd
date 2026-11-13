@echo off
if not defined JAVA_HOME (
    set "JAVA_HOME=C:\Program Files\Java\jdk-26.0.2.1"
)
set "PATH=%JAVA_HOME%\bin;%PATH%"
"C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.3\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" %*
