@echo off

:: 检查是否以管理员身份运行
net session >nul 2>&1
if %errorlevel% equ 0 (
    goto :start
) else (
    echo Requesting administrator privileges...
    goto :uac
)

:uac
:: 重新以管理员身份启动
set "batchArgs="
if "%*" neq "" set "batchArgs=%*"
powershell -Command "Start-Process cmd -ArgumentList '/c \"\"%~f0\" %batchArgs%\"' -Verb RunAs"
exit /b

:start
chcp 65001 >nul
title MySQL Service Manager (Admin)

echo.
echo ================================
echo    MySQL Service Manager
echo ================================
echo.

echo Checking MySQL95 service status...
sc query MySQL95 | find "RUNNING" >nul
if %errorlevel% equ 0 (
    echo STATUS: MySQL95 service is already running
    echo.
    pause
    exit /b
)

echo.
echo Starting MySQL95 service...
net start MySQL95
if %errorlevel% equ 0 (
    echo SUCCESS: MySQL95 service started successfully
) else (
    echo ERROR: Failed to start MySQL95 service
    echo.
    echo Possible reasons:
    echo - Service name is incorrect
    echo - MySQL is not installed
    echo - Service is disabled
)

echo.
pause