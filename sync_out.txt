@echo off
chcp 65001 >nul
echo ========================================
echo    GitHub 自动推送脚本
echo    仓库: warhammer43k
echo ========================================
echo.

:: 检查当前目录是否是Git仓库
git status >nul 2>&1
if errorlevel 1 (
    echo ⚠ 当前目录不是Git仓库，正在初始化...
    git init
    if errorlevel 1 (
        echo ❌ Git初始化失败，请确保已安装Git
        pause
        exit /b 1
    )
    echo ✅ Git仓库初始化成功
) else (
    echo ✅ 已在Git仓库中
)

echo.

:: 检查远程仓库配置
git remote get-url origin >nul 2>&1
if errorlevel 1 (
    echo 🔗 正在关联远程仓库...
    git remote add origin https://github.com/catleafleaf/warhammer43k.git
    if errorlevel 1 (
        echo ❌ 远程仓库关联失败
        pause
        exit /b 1
    )
    echo ✅ 远程仓库关联成功
) else (
    echo ✅ 远程仓库已关联
    git remote get-url origin
)

echo.

:: 添加所有文件到暂存区
echo 📁 正在添加文件到暂存区...
git add .
if errorlevel 1 (
    echo ❌ 文件添加失败
    pause
    exit /b 1
)
echo ✅ 文件添加成功

echo.

:: 提交更改
set /p commit_msg="💬 请输入提交说明: "
if "%commit_msg%"=="" (
    set commit_msg="自动提交：%date% %time%"
)

echo 📝 正在提交更改...
git commit -m "%commit_msg%"
if errorlevel 1 (
    echo ⚠ 提交失败，可能是没有更改需要提交
    echo.
)

echo.

:: 推送代码
echo 🚀 正在推送到GitHub...
git branch --show-current > current_branch.txt
set /p current_branch=<current_branch.txt
del current_branch.txt

echo 📤 推送到分支: %current_branch%

:: 尝试推送，如果失败则设置上游分支
git push -u origin %current_branch%
if errorlevel 1 (
    echo ⚠ 首次推送可能需要设置上游分支...
    git push --set-upstream origin %current_branch%
    if errorlevel 1 (
        echo ❌ 推送失败，请检查以下可能原因：
        echo   1. 网络连接问题
        echo   2. GitHub认证问题
        echo   3. 权限不足
        echo   4. 远程仓库不存在
        echo.
        echo 💡 解决方案：
        echo   - 检查GitHub账号密码/访问令牌
        echo   - 确认仓库URL正确
        echo   - 联系仓库管理员获取权限
        pause
        exit /b 1
    )
)

echo.
echo ========================================
echo ✅ 推送完成！
echo 📊 仓库地址: https://github.com/catleafleaf/warhammer43k
echo ========================================
echo.

:: 可选：打开浏览器查看仓库
set /p open_browser="是否在浏览器中打开仓库？(y/n): "
if /i "%open_browser%"=="y" (
    start https://github.com/catleafleaf/warhammer43k
)

pause