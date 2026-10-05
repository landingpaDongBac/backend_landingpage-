@echo off
setlocal

set MAVEN_VERSION=3.9.9
set WRAPPER_DIR=%~dp0.mvn\wrapper
set MAVEN_HOME=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%
set MAVEN_BIN=%MAVEN_HOME%\bin\mvn.cmd

if not exist "%MAVEN_BIN%" (
  if not exist "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "& { $ErrorActionPreference = 'Stop'; $url = 'https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip'; $zip = '%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%-bin.zip'; if (!(Test-Path -LiteralPath $zip)) { Invoke-WebRequest -Uri $url -OutFile $zip }; Expand-Archive -LiteralPath $zip -DestinationPath '%WRAPPER_DIR%' -Force }"
  if errorlevel 1 exit /b 1
)

"%MAVEN_BIN%" %*
