@echo off
set SDK_ROOT=%~dp0..\.tools\android-sdk
set APK=%~dp0..\app\build\outputs\apk\debug\app-debug.apk
"%SDK_ROOT%\platform-tools\adb.exe" wait-for-device
"%SDK_ROOT%\platform-tools\adb.exe" install -r "%APK%"
echo.
echo APK installed. Look for HealthDiary in the launcher.
pause
