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

if exist "D:\JDK\bin\java.exe" set "JAVA_EXE=D:\JDK\bin\java.exe"
if exist "D:\JDK\bin\javac.exe" set "JAVAC_EXE=D:\JDK\bin\javac.exe"

if not exist artifacts\reports mkdir artifacts\reports >nul 2>nul
if not exist dsl-output mkdir dsl-output >nul 2>nul

set "OUT_LOG=artifacts\reports\nl2dsl_agent_test.log"

echo [STEP 1/3] Compile natural-language to DSL agent test...
"%JAVAC_EXE%" -encoding UTF-8 -cp "build-classes;source" -sourcepath source -d build-classes source\integration\NaturalLanguageToDslAgentTest.java
if errorlevel 1 (
  echo [FAIL] Compile failed.
  exit /b 1
)

echo [STEP 2/3] Run agent conversion and print generated DSL...
if "%~1"=="" (
  "%JAVA_EXE%" -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -cp "build-classes;source" integration.NaturalLanguageToDslAgentTest > "%OUT_LOG%" 2>&1
) else (
  "%JAVA_EXE%" -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -cp "build-classes;source" integration.NaturalLanguageToDslAgentTest "%*" > "%OUT_LOG%" 2>&1
)
set "RUN_EXIT=%ERRORLEVEL%"
type "%OUT_LOG%"
if not "%RUN_EXIT%"=="0" (
  echo [FAIL] Conversion failed. See %OUT_LOG%
  exit /b 1
)

echo [STEP 3/3] Validate generated DSL file...
set "CHECK1=0"
set "CHECK2=0"
set "CHECK3=0"
set "CHECK4=0"
set "CHECK5=0"
set "CHECK6=0"
set "CHECK7=0"

findstr /I "agentReasoningTrace" dsl-output\agent_nl_to_dsl_latest.dsl >nul && set "CHECK1=1"
findstr /I "dataExchangeContract" dsl-output\agent_nl_to_dsl_latest.dsl >nul && set "CHECK2=1"
findstr /I "commandExchangeContract" dsl-output\agent_nl_to_dsl_latest.dsl >nul && set "CHECK3=1"
findstr /I "ddsIdlBinding" dsl-output\agent_nl_to_dsl_latest.dsl >nul && set "CHECK4=1"
findstr /I "routeDecision" dsl-output\agent_nl_to_dsl_latest.dsl >nul && set "CHECK5=1"
findstr /I "unitNormalizationContract" dsl-output\agent_nl_to_dsl_latest.dsl >nul && set "CHECK6=1"
findstr /I "synonymNormalizationContract" dsl-output\agent_nl_to_dsl_latest.dsl >nul && set "CHECK7=1"

echo CHECK agent_reasoning=!CHECK1!
echo CHECK data_contract=!CHECK2!
echo CHECK command_contract=!CHECK3!
echo CHECK dds_idl_binding=!CHECK4!
echo CHECK route_decision=!CHECK5!
echo CHECK unit_normalization=!CHECK6!
echo CHECK synonym_normalization=!CHECK7!

if "!CHECK1!!CHECK2!!CHECK3!!CHECK4!!CHECK5!!CHECK6!!CHECK7!"=="1111111" (
  echo [PASS] Agent NL-to-DSL conversion is healthy.
  echo [INFO] Latest DSL: dsl-output\agent_nl_to_dsl_latest.dsl
  echo [INFO] Full log: %OUT_LOG%
  exit /b 0
)

echo [FAIL] Missing required agent DSL signals. See %OUT_LOG%
exit /b 2
