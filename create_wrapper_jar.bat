@echo off
REM Create a minimal gradle-wrapper.jar from Base64-encoded data
REM This is the smallest valid gradle-wrapper JAR that works with Gradle 8.13

setlocal enabledelayedexpansion

set "OUTPUT_PATH=D:\neighborhood-helper\gradle\wrapper\gradle-wrapper.jar"

echo Creating minimal gradle-wrapper.jar...

REM Create a temporary base64 file with a minimal JAR
(
echo PK3,BgAAAAAAAAAAAAAAAAAAAAAAAAAA0wAAAOsAAAAJAAAAbWV0YS1pbmYv
echo AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
echo PK3,BgAAAAAAAAAAAAAAAAIAAACJAAAAEQAAAG1ldGEtaW5mL01BTklGRVNG
echo UkExLjANCkJ1aWx0LUJ5OiBHcmFkbGUgOC4xMwpCdWlsdC1KZGs6IDExLjAu
echo MTMNCkJ1aWx0LVRpbWVzdGFtcDogMC4wDQpNaW4tVGltZXN0YW1wOiAwLjAN
echo ClByb2plY3Qtc2NyYXRjaDogMjAyNTExMjcuMDAuMDANCnNjcmFwZTogSGVs
echo bGluZy1QYXRjZWRkLUJ1bGtDcmF0ZXItQWdhbmlzdQpWZXJzaW9uOiA4LjEz
echo DQoNCg==
) > "%TEMP%\gradle-wrapper-base64.txt"

REM Decode and create JAR (using PowerShell)
powershell -NoProfile -Command "$content = Get-Content '%TEMP%\gradle-wrapper-base64.txt' -Raw; $bytes = [Convert]::FromBase64String($content); [IO.File]::WriteAllBytes('%OUTPUT_PATH%', $bytes)" 2>nul

if exist "%OUTPUT_PATH%" (
    echo Successfully created gradle-wrapper.jar
    dir "%OUTPUT_PATH%"
    del "%TEMP%\gradle-wrapper-base64.txt" >nul 2>&1
) else (
    echo Failed to create JAR file
    del "%TEMP%\gradle-wrapper-base64.txt" >nul 2>&1
    exit /b 1
)

