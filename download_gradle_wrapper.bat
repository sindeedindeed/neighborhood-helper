@echo off
REM Download gradle-wrapper.jar from Maven Central

setlocal enabledelayedexpansion

set "MAVEN_URL=https://repo1.maven.org/maven2/gradle/wrapper/gradle-wrapper/gradle-wrapper.jar"
set "OUTPUT_PATH=D:\neighborhood-helper\gradle\wrapper\gradle-wrapper.jar"

echo Downloading gradle-wrapper.jar...
powershell -NoProfile -Command "(New-Object Net.WebClient).DownloadFile('%MAVEN_URL%', '%OUTPUT_PATH%')" 2>nul

if exist "%OUTPUT_PATH%" (
    echo Successfully downloaded gradle-wrapper.jar
    dir "%OUTPUT_PATH%"
    exit /b 0
) else (
    echo Failed to download gradle-wrapper.jar
    exit /b 1
)

