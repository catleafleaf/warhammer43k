@echo off
chcp 65001 >nul
title Git自动推送脚本

echo ========================================
echo          Git 延迟超时解决方案
echo ========================================
echo.

:: 检查是否在Git仓库中
git status >nul 2>&1
if errorlevel 1 (
    echo [错误] 当前目录不是Git仓库！
    pause
    exit /b 1
)

:: 设置Git配置以处理大文件或慢速连接
echo 正在优化Git配置以应对网络问题...
git config --global http.postBuffer 524288000
git config --global https.postBuffer 524288000
git config --global core.compression 9
git config --global http.lowSpeedLimit 0
git config --global http.lowSpeedTime 999999

:: 获取远程仓库名称
set REMOTE=origin
set BRANCH=main

:: 检查分支名称
git branch --show-current >nul 2>&1 && (
    for /f "tokens=*" %%i in ('git branch --show-current') do set BRANCH=%%i
)

echo 当前分支: %BRANCH%
echo.

:: 添加所有更改
echo [1/5] 添加文件变更到暂存区...
git add .
if errorlevel 1 (
    echo [错误] 添加文件失败！
    pause
    exit /b 1
)

:: 提交更改
set /p COMMIT_MSG=请输入提交信息: 
if "%COMMIT_MSG%"=="" set COMMIT_MSG="自动提交 %date% %time%"

echo [2/5] 提交更改...
git commit -m "%COMMIT_MSG%"
if errorlevel 1 (
    echo [警告] 提交失败或没有需要提交的更改
    echo 尝试直接推送...
    goto PUSH
)

:PUSH
echo [3/5] 开始推送到远程仓库...
echo 这可能会花费一些时间，请耐心等待...

:: 设置重试机制
set MAX_RETRY=3
set RETRY_COUNT=0

:RETRY_PUSH
set /a RETRY_COUNT+=1
echo.
echo [尝试第 %RETRY_COUNT% 次推送...]

:: 使用深度和超时设置进行推送
git -c http.postBuffer=524288000 -c http.lowSpeedTime=999999 -c http.lowSpeedLimit=0 push --progress %REMOTE% %BRANCH%

if errorlevel 1 (
    echo.
    echo [警告] 推送失败 (尝试 %RETRY_COUNT%/%MAX_RETRY%)
    
    if %RETRY_COUNT% lss %MAX_RETRY% (
        echo 等待10秒后重试...
        timeout /t 10 /nobreak >nul
        
        :: 尝试不同的推送策略
        if %RETRY_COUNT% equ 2 (
            echo 尝试使用浅层推送...
            git push --depth 1 %REMOTE% %BRANCH%
        ) else (
            goto RETRY_PUSH
        )
    ) else (
        echo.
        echo [错误] 经过 %MAX_RETRY% 次尝试后推送仍然失败
        echo 可能的原因:
        echo   - 网络连接问题
        echo   - 认证失败
        echo   - 远程仓库不存在
        echo   - 文件太大导致超时
        echo.
        echo 建议解决方案:
        echo  1. 检查网络连接
        echo  2. 确认GitHub令牌或密码正确
        echo  3. 尝试手动推送: git push origin %BRANCH%
        echo  4. 对于大文件，考虑使用Git LFS
        pause
        exit /b 1
    )
)

echo.
echo [4/5] 推送成功！
echo.

:: 显示最新状态
echo [5/5] 最终状态检查...
git status
echo.

echo ========================================
echo           推送完成！
echo ========================================
echo.
pause