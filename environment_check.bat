@echo off
chcp 65001 >nul
title 环境检查工具 - 战锤40K跑团系统
color 0D

echo.
echo ========================================
echo     环境检查工具 - 战锤40K跑团系统
echo ========================================
echo.

echo 正在检查系统环境...
echo.

:: 检查 Java
echo [1/5] 检查 Java 环境...
java -version >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Java 已安装
    java -version 2>&1 | find "version"
) else (
    echo ❌ Java 未安装或未配置环境变量
)

:: 检查 Maven
echo.
echo [2/5] 检查 Maven 环境...
mvn -version >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Maven 已安装
    mvn -version | find "Apache Maven"
) else (
    echo ❌ Maven 未安装或未配置环境变量
)

:: 检查 MySQL 服务
echo.
echo [3/5] 检查 MySQL 服务...
sc query MySQL95 >nul 2>&1
if %errorlevel% equ 0 (
    sc query MySQL95 | find "RUNNING" >nul
    if %errorlevel% equ 0 (
        echo ✅ MySQL95 服务正在运行
    ) else (
        echo ⚠ MySQL95 服务已安装但未运行
    )
) else (
    echo ❌ MySQL95 服务未安装
)

:: 检查 MySQL 连接
echo.
echo [4/5] 检查 MySQL 连接...
mysql -u root -pcatleafleaf070105! -e "SELECT 1;" >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ MySQL 连接正常
) else (
    echo ❌ MySQL 连接失败
)

:: 检查项目结构
echo.
echo [5/5] 检查项目结构...
if exist "src\main\java\Main.java" (
    echo ✅ 项目源代码存在
) else (
    echo ❌ 项目源代码缺失
)

if exist "pom.xml" (
    echo ✅ Maven 配置文件存在
) else (
    echo ❌ Maven 配置文件缺失
)

echo.
echo ========================================
echo 环境检查完成！
echo.
echo 使用说明:
echo 1. 运行 mysql_manager.bat 管理数据库
echo 2. 运行 start_project.bat 启动项目
echo 3. 运行 build_and_run.bat 重新编译并运行
echo 4. 运行 database_tools.bat 管理数据库数据
echo ========================================
echo.
pause