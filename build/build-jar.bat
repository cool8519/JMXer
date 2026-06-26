@echo off
setlocal EnableExtensions EnableDelayedExpansion

goto :main

:try_jdk17
set "_H=%~1"
if "%_H%"=="" exit /b 0
if not exist "%_H%\bin\javac.exe" exit /b 0
"%_H%\bin\javac.exe" -version 2>&1 | findstr /r /c:" 17\." >nul
if errorlevel 1 exit /b 0
set "JDK17_ROOT=%_H%"
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

:discover_jdk8
if defined JDK8_ROOT exit /b 0
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
if "%JDK8_ROOT%"=="" set "JDK8_ROOT="
exit /b 0

:copy_knowledge_resources
set "DEST_BASE=%~1\knowledge"
set "SRC_BASE=%ROOT%\knowledge"
if not exist "%DEST_BASE%" mkdir "%DEST_BASE%"
if not exist "%DEST_BASE%\mbeans" mkdir "%DEST_BASE%\mbeans"
if not exist "%SRC_BASE%" exit /b 0
if not exist "%SRC_BASE%\mbeans" exit /b 0
for %%F in ("%SRC_BASE%\mbeans\*.yaml" "%SRC_BASE%\mbeans\*.yml") do (
  if exist "%%~fF" copy /Y "%%~fF" "%DEST_BASE%\mbeans\" >nul
)
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

:merge_snakeyaml
set "MERGE_JAR=%~1"
if exist "%STAGE_YAML%" rd /s /q "%STAGE_YAML%"
mkdir "%STAGE_YAML%"
pushd "%STAGE_YAML%" >nul
jar xf "%SNAKEYAML_JAR%"
if exist META-INF\MANIFEST.MF del /f /q META-INF\MANIFEST.MF
popd >nul
jar uf "%MERGE_JAR%" -C "%STAGE_YAML%" .
set "MERGE_EC=%ERRORLEVEL%"
rd /s /q "%STAGE_YAML%"
exit /b %MERGE_EC%

:main

REM Fat JAR: output\JMXer.jar + output\JMXer-java8.jar
REM Run: build\build-jar.bat
REM JDK 17: JAVA17_HOME / JDK17_HOME / JAVA_HOME if 17, Program Files scan, C:\JAVA\*, or auto-download
REM JDK 8: JAVA8_HOME / JDK8_HOME / JAVA_HOME if 1.8, Program Files scan, C:\JAVA\*, or auto-download

set "ROOT=%~dp0.."
pushd "%ROOT%" >nul

set "LANT_VER=3.1.2"
set "SNAKEYAML_VER=2.2"
set "DEPS=%ROOT%\build\deps"
set "LANT_JAR=%DEPS%\lanterna-%LANT_VER%.jar"
set "SNAKEYAML_JAR=%DEPS%\snakeyaml-%SNAKEYAML_VER%.jar"
set "CLS=%ROOT%\build\classes"
set "CLS8=%ROOT%\build\classes-java8"
set "STAGE=%ROOT%\build\staging_lanterna"
set "STAGE_YAML=%ROOT%\build\staging_snakeyaml"
set "OUTDIR=%ROOT%\output"
set "MANIFEST=%ROOT%\build\fat-manifest.mf"
set "MANIFEST8=%ROOT%\build\fat-manifest-java8.mf"
set "LAUNCHER_SRC=%ROOT%\src\dal\tool\trace\jmxer\JmxLauncher.java"

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

if not exist "%SNAKEYAML_JAR%" (
  echo [build-jar] Downloading snakeyaml-%SNAKEYAML_VER%.jar ...
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/yaml/snakeyaml/%SNAKEYAML_VER%/snakeyaml-%SNAKEYAML_VER%.jar' -OutFile '%SNAKEYAML_JAR%' -UseBasicParsing"
  if errorlevel 1 (
    echo [build-jar] ERROR: failed to download SnakeYAML.
    popd & exit /b 1
  )
)

call :discover_jdk8
if not defined JDK8_ROOT (
  echo [build-jar] ERROR: Could not obtain JDK 8 ^(required for launcher compile; set JAVA8_HOME^).
  popd & exit /b 1
)

set "JDK17_ROOT="
call :try_jdk17 "%JAVA17_HOME%"
if not defined JDK17_ROOT call :try_jdk17 "%JDK17_HOME%"
if not defined JDK17_ROOT call :try_jdk17 "%JAVA_HOME%"
if not defined JDK17_ROOT (
  for /d %%D in ("%ProgramFiles%\Java\jdk-17*") do (
    if not defined JDK17_ROOT call :try_jdk17 "%%~fD"
  )
)
if not defined JDK17_ROOT (
  for /d %%D in ("%ProgramFiles%\Java\jdk17*") do (
    if not defined JDK17_ROOT call :try_jdk17 "%%~fD"
  )
)
if not defined JDK17_ROOT (
  for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk-17*") do (
    if not defined JDK17_ROOT call :try_jdk17 "%%~fD"
  )
)
if not defined JDK17_ROOT (
  for /d %%D in ("%ProgramFiles%\Microsoft\jdk-17*") do (
    if not defined JDK17_ROOT call :try_jdk17 "%%~fD"
  )
)
if not defined JDK17_ROOT (
  for /d %%D in ("%ProgramFiles%\Amazon Corretto\jdk17*") do (
    if not defined JDK17_ROOT call :try_jdk17 "%%~fD"
  )
)
if not "%ProgramFiles(x86)%"=="" (
  if not defined JDK17_ROOT (
    for /d %%D in ("%ProgramFiles(x86)%\Java\jdk-17*") do (
      if not defined JDK17_ROOT call :try_jdk17 "%%~fD"
    )
  )
)

if not defined JDK17_ROOT (
  if exist "C:\JAVA\" (
    for /d %%D in ("C:\JAVA\*") do (
      if not defined JDK17_ROOT call :try_jdk17 "%%~fD"
    )
  )
)

if not defined JDK17_ROOT (
  echo [build-jar] No local JDK 17 found - downloading JDK 17 into build\jdk17-runtime ^(cached - network needed once^)
  for /f "usebackq delims=" %%J in (`powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0ensure-jdk17.ps1" "%ROOT%"`) do set "JDK17_ROOT=%%J"
)
if not defined JDK17_ROOT (
  echo [build-jar] ERROR: Could not obtain JDK 17 ^(download or JAVA17_HOME^).
  popd & exit /b 1
)
if "%JDK17_ROOT%"=="" (
  echo [build-jar] ERROR: JDK 17 home path is empty ^(ensure-jdk17.ps1 may have failed^).
  popd & exit /b 1
)
if not exist "%JDK17_ROOT%\bin\javac.exe" (
  echo [build-jar] ERROR: javac.exe not found under "%JDK17_ROOT%"
  popd & exit /b 1
)

echo [build-jar] Compiling JMXer.jar with: %JDK17_ROOT%
if exist "%CLS%" rd /s /q "%CLS%"
mkdir "%CLS%"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0compile-java17.ps1" "%JDK17_ROOT%" "%LANT_JAR%" "%SNAKEYAML_JAR%" "%CLS%" "%ROOT%\src"
if errorlevel 1 (
  echo [build-jar] ERROR: Java 17 javac failed.
  popd & exit /b 1
)

echo [build-jar] Compiling launcher ^(Java 8 bytecode^) with: %JDK8_ROOT%
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0compile-launcher.ps1" "%JDK8_ROOT%" "%CLS%" "%LAUNCHER_SRC%"
if errorlevel 1 (
  echo [build-jar] ERROR: launcher javac failed.
  popd & exit /b 1
)

call :copy_knowledge_resources "%CLS%"
if errorlevel 1 (
  echo [build-jar] ERROR: failed to copy knowledge resources.
  popd & exit /b 1
)

echo Manifest-Version: 1.0> "%MANIFEST%"
echo Main-Class: dal.tool.trace.jmxer.JmxLauncher>> "%MANIFEST%"
echo JMXer-Min-Runtime: 17>> "%MANIFEST%"
echo JMXer-Alt-Jar: JMXer-java8.jar>> "%MANIFEST%"

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
call :merge_snakeyaml "%JAR_MAIN%"
if errorlevel 1 (
  echo [build-jar] ERROR: merging SnakeYAML into JMXer.jar failed.
  popd & exit /b 1
)
echo [build-jar] OK: %JAR_MAIN%

if not exist "%JDK8_ROOT%\bin\javac.exe" (
  echo [build-jar] ERROR: javac.exe not found under "%JDK8_ROOT%"
  popd & exit /b 1
)

echo [build-jar] Compiling JMXer-java8.jar with: %JDK8_ROOT%
if exist "%CLS8%" rd /s /q "%CLS8%"
mkdir "%CLS8%"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0compile-java8.ps1" "%JDK8_ROOT%" "%LANT_JAR%" "%SNAKEYAML_JAR%" "%CLS8%" "%ROOT%\src"
if errorlevel 1 (
  echo [build-jar] ERROR: Java 8 javac failed.
  popd & exit /b 1
)

call :copy_knowledge_resources "%CLS8%"
if errorlevel 1 (
  echo [build-jar] ERROR: failed to copy knowledge resources for Java 8.
  popd & exit /b 1
)

set "JAR8=%OUTDIR%\JMXer-java8.jar"
if exist "%JAR8%" del /f /q "%JAR8%"
echo Manifest-Version: 1.0> "%MANIFEST8%"
echo Main-Class: dal.tool.trace.jmxer.JmxLauncher>> "%MANIFEST8%"
>>"%MANIFEST8%" echo JMXer-Min-Runtime: 8
jar cfm "%JAR8%" "%MANIFEST8%" -C "%CLS8%" .
if errorlevel 1 (
  echo [build-jar] ERROR: jar create failed for Java 8.
  popd & exit /b 1
)
call :merge_lanterna "%JAR8%"
if errorlevel 1 (
  echo [build-jar] ERROR: merging Lanterna into JMXer-java8.jar failed.
  popd & exit /b 1
)
call :merge_snakeyaml "%JAR8%"
if errorlevel 1 (
  echo [build-jar] ERROR: merging SnakeYAML into JMXer-java8.jar failed.
  popd & exit /b 1
)
echo [build-jar] OK: %JAR8%

popd >nul
endlocal
exit /b 0
