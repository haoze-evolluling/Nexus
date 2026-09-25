@echo off
setlocal EnableExtensions

pushd "%~dp0android"
call build_apk.bat %*
set "EXIT_CODE=%ERRORLEVEL%"
popd
exit /b %EXIT_CODE%
