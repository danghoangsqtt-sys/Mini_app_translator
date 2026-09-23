@echo off
title Khoi dong May ao Android
echo ===================================================
echo   Dang khoi dong Dien thoai ao Android (AVD)...
echo ===================================================
start "" "%LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe" -avd medium_phone
