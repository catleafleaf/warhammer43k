@echo off
rem sync-repo-safe.bat - simple, robust, ASCII-only safe sync helper
rem Place this file in the repository root (the folder that contains .git) and run from CMD.
setlocal

:: Default remote and branch (user can change)
set REMOTE=origin
set /p BRANCH="Enter remote branch to sync (default master): "
if "%BRANCH%"=="" set BRANCH=master

echo ======================================================
echo sync-repo-safe.bat - Environment check...
echo Current directory: %cd%
echo Target: %REMOTE%/%BRANCH%
echo ======================================================

:: Check git availability
echo [CHECK] git --version ...
git --version >nul 2>&1
if errorlevel 1 goto no_git
echo OK: git detected.

echo.
echo [CHECK] where git ...
where git || echo (where failed)
echo.

:: Check .git presence
if not exist ".git" goto no_repo
echo OK: .git directory found.

echo.
echo [CHECK] Current branch:
git branch --show-current 2>nul || echo (Unable to detect current branch)
echo.

echo [CHECK] git remote -v:
git remote -v 2>nul
echo.

echo [CHECK] git fetch %REMOTE% (update refs only)...
git fetch %REMOTE% 2>nul
if errorlevel 1 (
  echo WARNING: git fetch returned non-zero. Remote name may be incorrect or network issue.
) else (
  echo git fetch succeeded (remote refs updated).
)
echo.

:menu
echo SAFE SYNC - target: %REMOTE%/%BRANCH%
echo.
echo 1 - Show remotes (git remote -v)
echo 2 - Show current branch (git branch --show-current)
echo 3 - Fetch remote refs (git fetch %REMOTE%)
echo 4 - Show incoming commits (git log --oneline HEAD..%REMOTE%/%BRANCH%)
echo 5 - Show files that would change (git diff --name-only HEAD..%REMOTE%/%BRANCH%)
echo 6 - Pull and merge (ensure working tree is clean)
echo 7 - Stash changes, pull --rebase, then pop stash
echo 8 - Pull with rebase (git pull --rebase)
echo 9 - Force reset to remote (DANGEROUS)
echo 10 - Backup current work to a temp branch
echo 11 - Exit
echo.
set /p opt="Choose an option number and press Enter: "

if "%opt%"=="1" goto show_remote
if "%opt%"=="2" goto show_branch
if "%opt%"=="3" goto fetch_remote
if "%opt%"=="4" goto show_incoming
if "%opt%"=="5" goto show_files
if "%opt%"=="6" goto safe_pull
if "%opt%"=="7" goto stash_pull
if "%opt%"=="8" goto rebase_pull
if "%opt%"=="9" goto force_reset
if "%opt%"=="10" goto backup_branch
if "%opt%"=="11" goto end
echo Invalid choice.
goto menu

:show_remote
echo === git remote -v ===
git remote -v
echo.
pause
goto menu

:show_branch
echo === Current local branch ===
git branch --show-current 2>nul || echo (Unable to detect current branch)
echo.
pause
goto menu

:fetch_remote
echo === git fetch %REMOTE% ===
git fetch %REMOTE%
if errorlevel 1 echo git fetch failed. Check network and remote configuration.
echo.
pause
goto menu

:show_incoming
echo === Incoming commits (remote ahead of local) ===
git fetch %REMOTE% 2>nul
git log --oneline HEAD..%REMOTE%/%BRANCH%
echo.
pause
goto menu

:show_files
echo === Files that would change ===
git fetch %REMOTE% 2>nul
git diff --name-only HEAD..%REMOTE%/%BRANCH%
echo.
pause
goto menu

:safe_pull
echo === git pull %REMOTE% %BRANCH% ===
echo Please ensure your working tree has no uncommitted changes (run git status to check).
set /p confirm="Proceed with pull and merge? (y/N): "
if /i not "%confirm%"=="y" goto menu
git pull %REMOTE% %BRANCH%
if errorlevel 1 echo Pull finished with errors. Inspect git output above.
echo.
pause
goto menu

:stash_pull
echo === Stash local changes -> git pull --rebase -> stash pop ===
set /p confirm="Proceed with stash + pull --rebase + pop? (y/N): "
if /i not "%confirm%"=="y" goto menu
git stash push -m "wip before sync" || (echo git stash failed & goto menu)
git pull --rebase %REMOTE% %BRANCH%
if errorlevel 1 (
  echo Rebase failed or conflicts occurred. You may need to run: git rebase --abort  and then git stash pop
  goto menu
)
git stash pop || echo (stash pop produced conflicts or no stash)
echo.
pause
goto menu

:rebase_pull
echo === git pull --rebase %REMOTE% %BRANCH% ===
set /p confirm="Proceed with pull --rebase on the current branch? (y/N): "
if /i not "%confirm%"=="y" goto menu
git fetch %REMOTE%
git pull --rebase %REMOTE% %BRANCH%
if errorlevel 1 echo Rebase encountered errors. Resolve conflicts and run: git add <file> && git rebase --continue
echo.
pause
goto menu

:force_reset
echo === DANGEROUS: Force reset local to %REMOTE%/%BRANCH% ===
set /p confirm="Type YES (uppercase) to confirm: "
if not "%confirm%"=="YES" goto menu
git fetch master
git checkout %BRANCH% 2>nul || (echo Failed to switch to branch %BRANCH% & goto menu)
git reset --hard %REMOTE%/%BRANCH%
git clean -fd
echo Force sync completed.
echo(
pause
goto menu

:backup_branch
echo === Backup current work to a temporary branch ===
for /f "tokens=1-4 delims=/ " %%a in ("%date%") do set dt=%%c%%a%%b
for /f "tokens=1-2 delims=:." %%a in ("%time%") do set tm=%%a%%b
set timestamp=%dt%_%tm%
set /p branchName="Enter a suffix for backup branch (default: auto): "
if "%branchName%"=="" set branchName=auto
set newBranch=backup/wip-%branchName%-%timestamp%
echo Creating branch %newBranch% and committing any changes...
git checkout -b "%newBranch%" 2>nul
git add -A
git commit -m "WIP backup before sync (%timestamp%)" 2>nul
if errorlevel 1 echo Maybe no changes to commit or commit failed. Branch %newBranch% still created.
git status --short
echo(
pause
goto menu

:end
echo Done.
echo Press any key to exit...
pause >nul
endlocal
exit /b 0