@echo off
chcp 65001 >nul
cd /d "%~dp0"
setlocal

set "JAVA_EXE=java"
set "JAVAC_EXE=javac"
if not "%JAVA_HOME%"=="" (
  if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
  if exist "%JAVA_HOME%\bin\javac.exe" set "JAVAC_EXE=%JAVA_HOME%\bin\javac.exe"
)

if not exist artifacts\reports mkdir artifacts\reports >nul 2>nul

echo [STEP 1/2] Compile route fusion comparative experiment...
"%JAVAC_EXE%" -encoding UTF-8 -cp "build-classes;generated-src;lib/*;source" -sourcepath source -d build-classes source\integration\RouteFusionExperiment.java
if errorlevel 1 (
  echo [FAIL] Compile failed.
  pause
  exit /b 1
)

echo [STEP 2/2] Run QoS / latency / conflict experiment...
"%JAVA_EXE%" -cp build-classes integration.RouteFusionExperiment
if errorlevel 1 (
  echo [FAIL] Experiment failed.
  pause
  exit /b 1
)

echo.
echo [PASS] Experiment completed. Reports are in artifacts\reports.
pause
exit /b 0
