@echo off
set JAVA_HOME=%~dp0..\.tools\jdk\zulu17.52.17-ca-jdk17.0.12-win_x64
set ANDROID_HOME=%~dp0..\.tools\android-sdk
set ANDROID_SDK_ROOT=%ANDROID_HOME%
call "%~dp0..\.tools\gradle\gradle-8.11.1\bin\gradle.bat" :app:assembleDebug
pause
