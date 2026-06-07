@echo off
chcp 65001 >nul
cd /d "%~dp0"
setlocal enabledelayedexpansion

set "JAVA_EXE=java"
set "JAVAC_EXE=javac"
if not "%JAVA_HOME%"=="" (
  if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
  if exist "%JAVA_HOME%\bin\javac.exe" set "JAVAC_EXE=%JAVA_HOME%\bin\javac.exe"
)

if not exist artifacts\reports mkdir artifacts\reports >nul 2>nul
set "OUT_LOG=artifacts\reports\virtual_dds_scene_test.log"

echo [STEP 1/3] Compile virtual DDS scene classes...
"%JAVAC_EXE%" -encoding UTF-8 -cp "build-classes;generated-src;lib/*;source" -sourcepath source -d build-classes source\integration\VirtualDDSScenarioDemo.java
if errorlevel 1 (
  echo [FAIL] Compile failed.
  exit /b 1
)

echo [STEP 2/3] Run virtual DDS scene demo...
set "PS_RUN=$ErrorActionPreference='Stop'; $OutputEncoding=[Console]::OutputEncoding=[System.Text.UTF8Encoding]::new($false); [Console]::OutputEncoding=[System.Text.UTF8Encoding]::new($false); $javaArgs=@('-Dfile.encoding=UTF-8','-Dsun.stdout.encoding=UTF-8','-Dsun.stderr.encoding=UTF-8','-cp','build-classes;generated-src;lib/*','integration.VirtualDDSScenarioDemo','12'); Set-Content -LiteralPath '%OUT_LOG%' -Value '' -Encoding utf8; & '%JAVA_EXE%' @javaArgs 2>&1 | ForEach-Object { $line = $_.ToString(); Add-Content -LiteralPath '%OUT_LOG%' -Value $line -Encoding utf8; Write-Host $line }; if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }"
powershell -NoProfile -ExecutionPolicy Bypass -Command "%PS_RUN%"
if errorlevel 1 (
  echo [FAIL] Demo run failed. See %OUT_LOG%
  exit /b 1
)

echo [STEP 3/3] Validate DDS dialogue signals...
set "CHECK1=0"
set "CHECK2=0"
set "CHECK3=0"
set "CHECK4=0"
set "CHECK5=0"
set "CHECK6=0"
set "CHECK7=0"
set "CHECK8=0"

findstr /I "PASSENGER_INTENT" "%OUT_LOG%" >nul && set "CHECK1=1"
findstr /I "ROUTE_ADVICE" "%OUT_LOG%" >nul && set "CHECK2=1"
findstr /I "RSU_ROAD_EVENT" "%OUT_LOG%" >nul && set "CHECK3=1"
findstr /I "COMPLIANT_CONTEXT" "%OUT_LOG%" >nul && set "CHECK4=1"
findstr /I "FINAL_ROUTE_DECISION" "%OUT_LOG%" >nul && set "CHECK5=1"
findstr /I "dataExchangeContract" "%OUT_LOG%" >nul && set "CHECK6=1"
findstr /I "commandExchangeContract" "%OUT_LOG%" >nul && set "CHECK7=1"
findstr /I "ddsIdlBinding" "%OUT_LOG%" >nul && set "CHECK8=1"

echo CHECK passenger_intent=!CHECK1!
echo CHECK route_advice=!CHECK2!
echo CHECK rsu_event=!CHECK3!
echo CHECK compliant_context=!CHECK4!
echo CHECK final_decision=!CHECK5!
echo CHECK dsl_data_contract=!CHECK6!
echo CHECK dsl_command_contract=!CHECK7!
echo CHECK dds_idl_binding=!CHECK8!

if "!CHECK1!!CHECK2!!CHECK3!!CHECK4!!CHECK5!!CHECK6!!CHECK7!!CHECK8!"=="11111111" (
  echo [PASS] Virtual DDS scene communication is healthy.
  echo [INFO] Full log: %OUT_LOG%
  exit /b 0
)

echo [FAIL] Missing required DDS/DSL dialogue signals. See %OUT_LOG%
exit /b 2

