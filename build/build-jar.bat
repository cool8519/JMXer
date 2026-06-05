@echo off
setlocal EnableExtensions EnableDelayedExpansion

goto :main_start

:main_start

REM Fat JAR: output\JMXer.jar + output\JMXer-java8.jar
REM Run: build\build-jar.bat
REM JDK 8: JAVA8_HOME / JDK8_HOME / JAVA_HOME if 1.8, Program Files scan, C:\JAVA\*, or auto-download

set "ROOT=%~dp0.."
pushd "%ROOT%" >nul

set "LANT_VER=3.1.2"
set "DEPS=%ROOT%\build\deps"
set "LANT_JAR=%DEPS%\lanterna-%LANT_VER%.jar"
set "CLS=%ROOT%\build\classes"
set "CLS8=%ROOT%\build\classes-java8"
set "STAGE=%ROOT%\build\staging_lanterna"
set "OUTDIR=%ROOT%\output"
set "MANIFEST=%ROOT%\build\fat-manifest.mf"

if not exist "%DEPS%" mkdir "%DEPS%"
if not exist "%CLS%" mkdir "%CLS%"
if not exist "%OUTDIR%" mkdir "%OUTDIR%"

if not exist "%LANT_JAR%" (
  echo [build-jar] Downloading lanterna-%LANT_VER%.jar ...
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/googlecode/lanterna/lanterna/%LANT_VER%/lanterna-%LANT_VER%.jar' -OutFile '%LANT_JAR%' -UseBasicParsing"
  if errorlevel 1 (
    echo [build-jar] ERROR: failed to download Lanterna.
    popd & exit /b 1
  )
)

echo [build-jar] Compiling JMXer.jar ^(default JDK on PATH^) ...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Stop'; javac -encoding UTF-8 -cp '%LANT_JAR%' -d '%CLS%' @(Get-ChildItem -Recurse -Path '%ROOT%\src' -Filter '*.java' | ForEach-Object { $_.FullName })"
if errorlevel 1 (
  echo [build-jar] ERROR: javac failed.
  popd & exit /b 1
)

echo Manifest-Version: 1.0> "%MANIFEST%"
echo Main-Class: dal.tool.trace.jmxer.JMXControl>> "%MANIFEST%"

set "JAR_MAIN=%OUTDIR%\JMXer.jar"
if exist "%JAR_MAIN%" del /f /q "%JAR_MAIN%"
jar cfm "%JAR_MAIN%" "%MANIFEST%" -C "%CLS%" .
if errorlevel 1 (
  echo [build-jar] ERROR: jar create failed.
  popd & exit /b 1
)

call :merge_lanterna "%JAR_MAIN%"
if errorlevel 1 (
  echo [build-jar] ERROR: merging Lanterna into JMXer.jar failed.
  popd & exit /b 1
)
echo [build-jar] OK: %JAR_MAIN%

set "JDK8_ROOT="
call :try_jdk8 "%JAVA8_HOME%"
if not defined JDK8_ROOT call :try_jdk8 "%JDK8_HOME%"
if not defined JDK8_ROOT call :try_jdk8 "%JAVA_HOME%"
if not defined JDK8_ROOT (
  for /d %%D in ("%ProgramFiles%\Java\jdk1.8*") do (
    if not defined JDK8_ROOT call :try_jdk8 "%%~fD"
  )
)
if not defined JDK8_ROOT (
  for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk-8*") do (
    if not defined JDK8_ROOT call :try_jdk8 "%%~fD"
  )
)
if not defined JDK8_ROOT (
  for /d %%D in ("%ProgramFiles%\Microsoft\jdk-8*") do (
    if not defined JDK8_ROOT call :try_jdk8 "%%~fD"
  )
)
if not defined JDK8_ROOT (
  for /d %%D in ("%ProgramFiles%\Amazon Corretto\jdk1.8*") do (
    if not defined JDK8_ROOT call :try_jdk8 "%%~fD"
  )
)
if not "%ProgramFiles(x86)%"=="" (
  if not defined JDK8_ROOT (
    for /d %%D in ("%ProgramFiles(x86)%\Java\jdk1.8*") do (
      if not defined JDK8_ROOT call :try_jdk8 "%%~fD"
    )
  )
)

if not defined JDK8_ROOT (
  if exist "C:\JAVA\" (
    for /d %%D in ("C:\JAVA\*") do (
      if not defined JDK8_ROOT call :try_jdk8 "%%~fD"
    )
  )
)

if not defined JDK8_ROOT (
  echo [build-jar] No local JDK 8 found - downloading JDK 8 into build\jdk8-runtime ^(cached - network needed once^)
  for /f "usebackq delims=" %%J in (`powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0ensure-jdk8.ps1" "%ROOT%"`) do set "JDK8_ROOT=%%J"
)
if not defined JDK8_ROOT (
  echo [build-jar] ERROR: Could not obtain JDK 8 ^(download or JAVA8_HOME^).
  popd & exit /b 1
)
if "%JDK8_ROOT%"=="" (
  echo [build-jar] ERROR: JDK 8 home path is empty ^(ensure-jdk8.ps1 may have failed^).
  popd & exit /b 1
)
if not exist "%JDK8_ROOT%\bin\javac.exe" (
  echo [build-jar] ERROR: javac.exe not found under "%JDK8_ROOT%"
  popd & exit /b 1
)

echo [build-jar] Compiling JMXer-java8.jar with: %JDK8_ROOT%
if exist "%CLS8%" rd /s /q "%CLS8%"
mkdir "%CLS8%"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0compile-java8.ps1" "%JDK8_ROOT%" "%LANT_JAR%" "%CLS8%" "%ROOT%\src"
if errorlevel 1 (
  echo [build-jar] ERROR: Java 8 javac failed.
  popd & exit /b 1
)

set "JAR8=%OUTDIR%\JMXer-java8.jar"
if exist "%JAR8%" del /f /q "%JAR8%"
jar cfm "%JAR8%" "%MANIFEST%" -C "%CLS8%" .
if errorlevel 1 (
  echo [build-jar] ERROR: jar create failed for Java 8.
  popd & exit /b 1
)
call :merge_lanterna "%JAR8%"
if errorlevel 1 (
  echo [build-jar] ERROR: merging Lanterna into JMXer-java8.jar failed.
  popd & exit /b 1
)
echo [build-jar] OK: %JAR8%

popd >nul
endlocal
exit /b 0

:try_jdk8
set "_H=%~1"
if "%_H%"=="" exit /b 0
if not exist "%_H%\bin\javac.exe" exit /b 0
if not exist "%_H%\lib\tools.jar" exit /b 0
"%_H%\bin\javac.exe" -version 2>&1 | findstr /r /c:"1\.8\." >nul
if errorlevel 1 exit /b 0
set "JDK8_ROOT=%_H%"
exit /b 0

:merge_lanterna
set "MERGE_JAR=%~1"
if exist "%STAGE%" rd /s /q "%STAGE%"
mkdir "%STAGE%"
pushd "%STAGE%" >nul
jar xf "%LANT_JAR%"
if exist META-INF\MANIFEST.MF del /f /q META-INF\MANIFEST.MF
popd >nul
jar uf "%MERGE_JAR%" -C "%STAGE%" .
set "MERGE_EC=%ERRORLEVEL%"
rd /s /q "%STAGE%"
exit /b %MERGE_EC%
