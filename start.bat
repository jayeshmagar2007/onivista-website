@echo off
setlocal EnableExtensions
cd /d "%~dp0"
title ONIVISTA AGRO - Java Server
color 0A
cls
echo ============================================
echo        ONIVISTA AGRO - JAVA WEBSITE
echo ============================================
echo.
echo Project folder: %CD%
echo.
if not exist "%~dp0OnivistaAgroServer.java" goto NOSOURCE
where java >nul 2>nul
if errorlevel 1 goto NOJAVA
where javac >nul 2>nul
if errorlevel 1 goto NOJAVAC
java -version
if errorlevel 1 goto NOJAVA

echo.
echo [1/4] Checking port 3000...
for /f "tokens=5" %%P in ('netstat -ano ^| findstr /R /C:":3000 .*LISTENING"') do (
  echo Stopping old process on port 3000: %%P
  taskkill /PID %%P /F >nul 2>nul
)
echo [2/4] Preparing review storage...
if not exist "%~dp0data" mkdir "%~dp0data"
if not exist "%~dp0data\reviews.json" echo []>"%~dp0data\reviews.json"
echo [3/4] Compiling Java backend...
javac -encoding UTF-8 "%~dp0OnivistaAgroServer.java"
if errorlevel 1 goto COMPILEFAIL
if not exist "%~dp0OnivistaAgroServer.class" goto COMPILEFAIL
echo [4/4] Starting website...
echo.
echo Website : http://localhost:3000
echo Admin   : http://localhost:3000/admin.html
echo Password: onivista123
echo.
echo IMPORTANT: Keep this window open.
start "" "http://localhost:3000"
java -cp "%~dp0" OnivistaAgroServer
echo.
echo The Java server has stopped.
pause
exit /b 0
:NOSOURCE
echo ERROR: OnivistaAgroServer.java is missing.
echo Extract the ENTIRE ZIP file into one folder. Do not copy start.bat alone.
echo Expected file: %~dp0OnivistaAgroServer.java
pause
exit /b 1
:NOJAVA
echo ERROR: Java was not found. Install JDK 17 or newer.
pause
exit /b 1
:NOJAVAC
echo ERROR: javac was not found. Install a JDK 17 or newer, not only the Java runtime.
pause
exit /b 1
:COMPILEFAIL
echo ERROR: Java compilation failed. Read the compiler message above.
pause
exit /b 1
