@echo off
rem Run this file AS ADMINISTRATOR once to enable emulator acceleration.
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo Please right-click this file and choose "Run as administrator".
    pause
    exit /b 1
)
set SDK_ROOT=%~dp0..\.tools\android-sdk
cd /d "%SDK_ROOT%\extras\google\Android_Emulator_Hypervisor_Driver"
call silent_install.bat
echo.
echo Driver install finished. You can now run start-emulator.bat
pause
