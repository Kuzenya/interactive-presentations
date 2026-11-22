@echo off
cd /d "%~dp0"
if not exist "target\interactive-presentations-1.0.0.jar" call mvn package
if errorlevel 1 exit /b 1
java -Dfile.encoding=UTF-8 -jar target\interactive-presentations-1.0.0.jar %*
