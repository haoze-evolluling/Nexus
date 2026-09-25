@echo off
setlocal EnableExtensions

rem Build both SyncTouch artifacts (Android APK and Windows Desktop) from repository root.
pushd "%~dp0"

set "PAUSE_AT_END=1"
if /i "%~1"=="--no-pause" set "PAUSE_AT_END=0"

set "BUILD_EXIT=1"
set "ANDROID_APK=app\build\outputs\apk\debug\app-debug.apk"

echo ========================================================
echo  [1/2] Building SyncTouch Android Debug APK...
echo ========================================================
call "gradlew.bat" assembleDebug
set "ANDROID_EXIT=%ERRORLEVEL%"
if not "%ANDROID_EXIT%"=="0" (
    echo [ERROR] Android build failed.
    goto :done
)

if not exist "%ANDROID_APK%" (
    echo [ERROR] Android build completed but APK was not found:
    echo         %ANDROID_APK%
    goto :done
)
echo [OK] Android APK: %ANDROID_APK%
echo.

echo ========================================================
echo  [2/2] Building SyncTouch Desktop Windows Artifact...
echo ========================================================
if exist "desktop\build.bat" (
    call "desktop\build.bat" --no-pause
    if errorlevel 1 (
        echo [ERROR] Windows desktop build failed.
        goto :done
    )
    echo [OK] Windows desktop build completed.
) else (
    echo [WARN] desktop\build.bat not found, skipping desktop build.
)

set "BUILD_EXIT=0"

:done
popd
if "%PAUSE_AT_END%"=="1" (
    echo.
    if "%BUILD_EXIT%"=="0" (
        echo All builds finished successfully!
    ) else (
        echo Build stopped with errors. Review the messages above.
    )
    pause
)
exit /b %BUILD_EXIT%
