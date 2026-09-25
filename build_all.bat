@echo off
setlocal EnableExtensions EnableDelayedExpansion

rem ====================================================================
rem  Nexus Unified Cross-Platform Build Script
rem  - Android: Debug build signed with Release Keystore
rem  - Desktop: Windows executable and installer
rem  - Output: All artifacts gathered into %ROOT_DIR%output\
rem ====================================================================

pushd "%~dp0"
set "ROOT_DIR=%~dp0"
set "OUTPUT_DIR=%ROOT_DIR%output"

set "PAUSE_AT_END=1"
if /i "%~1"=="--no-pause" set "PAUSE_AT_END=0"

set "BUILD_EXIT=1"

if not exist "%OUTPUT_DIR%" (
    mkdir "%OUTPUT_DIR%" 2>nul
)

echo.
echo ========================================================
echo   Nexus Unified Cross-Platform Build Tool
echo   Output Directory: %OUTPUT_DIR%
echo ========================================================
echo.

rem --------------------------------------------------------------------
rem  [1/2] Build Android Debug APK (Release Keystore Signed)
rem --------------------------------------------------------------------
echo ========================================================
echo  [1/2] Building Android Debug APK (Release Keystore)...
echo ========================================================

if not exist "%ROOT_DIR%android\gradlew.bat" (
    echo [ERROR] android\gradlew.bat not found.
    goto :done
)

pushd "%ROOT_DIR%android"
echo [INFO] Running: gradlew.bat assembleDebug -PsignDebugWithRelease=true
call gradlew.bat assembleDebug -PsignDebugWithRelease=true
set "ANDROID_EXIT=%ERRORLEVEL%"
popd

if not "%ANDROID_EXIT%"=="0" (
    echo [ERROR] Android build failed with exit code %ANDROID_EXIT%.
    goto :done
)

set "FOUND_APK="
set "VERSIONED_APK="

for /f "delims=" %%F in ('dir /b /s /o:-d "%ROOT_DIR%android\app\build\outputs\apk\versioned\debug\Nexus-debug-v*.apk" 2^>nul') do (
    if not defined VERSIONED_APK set "VERSIONED_APK=%%F"
)

if exist "%ROOT_DIR%android\app\build\outputs\apk\debug\app-debug.apk" (
    set "FOUND_APK=%ROOT_DIR%android\app\build\outputs\apk\debug\app-debug.apk"
)

if defined VERSIONED_APK (
    copy /y "%VERSIONED_APK%" "%OUTPUT_DIR%\" >nul
    echo [OK] Copied versioned APK to output: !VERSIONED_APK!
)

if defined FOUND_APK (
    copy /y "%FOUND_APK%" "%OUTPUT_DIR%\Nexus-debug.apk" >nul
    echo [OK] Copied debug APK to output: %OUTPUT_DIR%\Nexus-debug.apk
) else (
    if not defined VERSIONED_APK (
        echo [ERROR] Android build finished but APK was not found.
        goto :done
    )
)

echo [OK] Android Debug APK build completed successfully.
echo.

rem --------------------------------------------------------------------
rem  [2/2] Build Windows Desktop (Wails + Vue)
rem --------------------------------------------------------------------
echo ========================================================
echo  [2/2] Building Nexus Desktop Windows Artifacts...
echo ========================================================

if not exist "%ROOT_DIR%desktop\build.bat" (
    echo [WARN] desktop\build.bat not found, skipping desktop build.
    goto :skip_desktop
)

pushd "%ROOT_DIR%desktop"
call build.bat --no-pause
set "DESKTOP_EXIT=%ERRORLEVEL%"
popd

if not "%DESKTOP_EXIT%"=="0" (
    echo [ERROR] Windows desktop build failed with exit code %DESKTOP_EXIT%.
    goto :done
)

if exist "%ROOT_DIR%desktop\build\bin\Nexus.exe" (
    copy /y "%ROOT_DIR%desktop\build\bin\Nexus.exe" "%OUTPUT_DIR%\Nexus.exe" >nul
    echo [OK] Copied Desktop exe to output: %OUTPUT_DIR%\Nexus.exe
)

for /f "delims=" %%I in ('dir /b /a-d "%ROOT_DIR%desktop\build\bin\*installer*.exe" 2^>nul') do (
    copy /y "%ROOT_DIR%desktop\build\bin\%%I" "%OUTPUT_DIR%\" >nul
    echo [OK] Copied Desktop installer to output: %OUTPUT_DIR%\%%I
)

echo [OK] Windows Desktop build completed successfully.
echo.

:skip_desktop
set "BUILD_EXIT=0"

:done
echo.
echo ========================================================
if "%BUILD_EXIT%"=="0" (
    echo  [SUCCESS] All builds finished successfully!
    echo.
    echo  Unified Output Directory:
    echo    %OUTPUT_DIR%
    echo.
    echo  Artifacts currently in output folder:
    dir /b /a-d "%OUTPUT_DIR%" 2>nul
) else (
    echo  [FAILED] Build stopped with errors. Check logs above.
)
echo ========================================================

popd
if "%PAUSE_AT_END%"=="1" (
    echo.
    pause
)
exit /b %BUILD_EXIT%
