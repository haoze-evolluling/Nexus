@echo off
setlocal EnableExtensions

rem Build the SteamVoice Windows installer from this script's directory.
pushd "%~dp0"

set "PAUSE_AT_END=1"
if /i "%~1"=="--no-pause" set "PAUSE_AT_END=0"

set "BUILD_EXIT=1"

where wails >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Wails CLI was not found on PATH.
    echo Install it with: go install github.com/wailsapp/wails/v2/cmd/wails@latest
    goto :done
)

where go >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Go was not found on PATH.
    goto :done
)

where npm >nul 2>nul
if errorlevel 1 (
    echo [ERROR] npm was not found on PATH.
    goto :done
)

where makensis >nul 2>nul
if errorlevel 1 (
    echo [ERROR] NSIS makensis was not found on PATH.
    echo Install NSIS and add its installation directory to PATH.
    goto :done
)

where pkg-config >nul 2>nul
if errorlevel 1 (
    echo [ERROR] pkg-config was not found on PATH.
    goto :done
)

pkg-config --exists opus
if errorlevel 1 (
    echo [ERROR] libopus development files were not found by pkg-config.
    echo Install libopus headers and library files, then try again.
    goto :done
)

set "CGO_ENABLED=1"
set "CGO_LDFLAGS=-Wl,-Bstatic -lopus -Wl,-Bdynamic %CGO_LDFLAGS%"
echo Building SteamVoice Windows installer...
wails build -clean -nsis -installscope machine -tags "steamvoice_opus nolibopusfile"
if errorlevel 1 (
    echo [ERROR] Desktop installer build failed.
    goto :done
)

set "INSTALLER="
for /f "delims=" %%I in ('dir /b /a-d "build\bin\*installer*.exe" 2^>nul') do set "INSTALLER=build\bin\%%I"
if not defined INSTALLER (
    echo [ERROR] Build completed but no NSIS installer was found in build\bin.
    goto :done
)

echo [OK] Installer build completed.
echo [OK] Output: %INSTALLER%
set "BUILD_EXIT=0"

:done
popd
if "%PAUSE_AT_END%"=="1" (
    echo.
    if "%BUILD_EXIT%"=="0" (
        echo Build finished successfully.
    ) else (
        echo Build stopped with errors. Review the message above.
    )
    pause
)
exit /b %BUILD_EXIT%
