@echo off
REM Starts the migration application.
REM This script assumes that Java 21 is installed and available in PATH.

SET SCRIPT_PATH=%~dp0
IF "%SCRIPT_PATH:~-1%"=="\" SET SCRIPT_PATH=%SCRIPT_PATH:~0,-1%
SET APP_PATH=%SCRIPT_PATH%

pushd "%APP_PATH%"
IF ERRORLEVEL 1 (
    echo Failed to change directory to %APP_PATH%
    exit /b 1
)

java -jar app.jar -config=application.conf
popd