@echo off
REM Download gradle-wrapper.jar from GitHub mirror (faster)
setlocal enabledelayedexpansion

set "GITHUB_URL=https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar"
set "OUTPUT_PATH=D:\neighborhood-helper\gradle\wrapper\gradle-wrapper.jar"
set "TEMP_FILE=%TEMP%\gradle-wrapper-temp.jar"

echo Attempting download from GitHub mirror...
powershell -NoProfile -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object Net.WebClient).DownloadFile('https://github.com/gradle/gradle/raw/master/gradle/wrapper/gradle-wrapper.jar', '%TEMP_FILE%')" >nul 2>&1

if exist "%TEMP_FILE%" (
    move /Y "%TEMP_FILE%" "%OUTPUT_PATH%" >nul 2>&1
    if exist "%OUTPUT_PATH%" (
        echo Successfully downloaded gradle-wrapper.jar
        dir "%OUTPUT_PATH%"
        exit /b 0
    )
)

echo Failed to download from GitHub, trying Maven Central mirror...
powershell -NoProfile -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object Net.WebClient).DownloadFile('https://jcenter.bintray.com/gradle/wrapper/gradle-wrapper/gradle-wrapper.jar', '%OUTPUT_PATH%')" >nul 2>&1

if exist "%OUTPUT_PATH%" (
    echo Successfully downloaded gradle-wrapper.jar from Bintray
    dir "%OUTPUT_PATH%"
    exit /b 0
)

echo All download attempts failed. Please use IntelliJ built-in Gradle instead.
echo See GRADLE_WRAPPER_FIX.md for instructions.
exit /b 1

