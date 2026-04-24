@echo off
setlocal

REM ─── JDK ───────────────────────────────────────────────────────────────
if not defined JAVA_HOME (
    if exist "C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot" (
        set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot"
    ) else (
        for /f "delims=" %%i in ('dir /b /ad "C:\Program Files\Eclipse Adoptium" 2^>nul ^| findstr /i "^jdk-"') do (
            set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\%%i"
        )
    )
)

if not defined JAVA_HOME (
    echo [ERROR] JAVA_HOME not found. Install a JDK or set JAVA_HOME manually.
    pause
    exit /b 1
)

echo Using JAVA_HOME=%JAVA_HOME%

REM ─── Yandex SMTP creds (OTP emails) ────────────────────────────────────
set "MAIL_USERNAME=kotyanyak@yandex.ru"
set "MAIL_PASSWORD=mtmoiftktpbjenim"

REM ─── Console encoding + English locale for logs ────────────────────────
chcp 65001 >nul
set "JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -Duser.language=en -Duser.country=US"

REM ─── Launch ────────────────────────────────────────────────────────────
cd /d "%~dp0"
call mvnw.cmd spring-boot:run

endlocal
pause
