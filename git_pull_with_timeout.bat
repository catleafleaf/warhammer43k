@echo off
chcp 65001 >nul
echo ========================================
echo    Git Pull with Timeout Setting
echo ========================================
echo.

echo [1/2] 设置Git全局超时时间为300秒...
git config --global http.timeout 300000

if %errorlevel% neq 0 (
    echo ❌ 错误: 设置超时时间失败
    pause
    exit /b 1
)

echo ✅ 超时时间设置成功
echo.

echo [2/2] 执行 git pull origin master...
echo ----------------------------------------
git pull origin master

if %errorlevel% neq 0 (
    echo ❌ 错误: Git pull 执行失败
) else (
    echo ✅ Git pull 执行成功
)

echo.
echo ----------------------------------------
echo 操作完成
pause