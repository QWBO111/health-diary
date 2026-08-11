@echo off
set SDK_ROOT=%~dp0..\.tools\android-sdk
echo Starting emulator: HealthDiary (Android 15 / Pixel 6)...
"%SDK_ROOT%\emulator\emulator.exe" -avd HealthDiary -gpu auto
