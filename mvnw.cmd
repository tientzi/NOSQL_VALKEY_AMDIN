@echo off
@setlocal
chcp 65001 > nul 2>&1

@REM Ensure Java 21/17 is used for Spring Boot 3
if not exist "%JAVA_HOME%\bin\java.exe" (
    if exist "C:\Program Files\Java\jdk-21.0.11\bin\java.exe" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.11"
    ) else if exist "C:\Program Files\Java\jdk-17\bin\java.exe" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-17"
    )
) else (
    "%JAVA_HOME%\bin\java.exe" -version 2>&1 | findstr /i "1.8" > nul
    if not errorlevel 1 (
        if exist "C:\Program Files\Java\jdk-21.0.11\bin\java.exe" (
            set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.11"
        ) else if exist "C:\Program Files\Java\jdk-17\bin\java.exe" (
            set "JAVA_HOME=C:\Program Files\Java\jdk-17"
        )
    )
)

if defined JAVA_HOME (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

set "JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8"

@REM 1. Fast path: Find existing extracted Maven in local .m2 repository
for /d %%D in ("%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9*") do (
    for /r "%%D" %%F in (mvn.cmd) do (
        if exist "%%F" (
            call "%%F" %*
            exit /b %ERRORLEVEL%
        )
    )
)

@REM 2. Wrapper Jar with relative path and Main class
if exist ".mvn\wrapper\maven-wrapper.jar" (
    java -Dfile.encoding=UTF-8 "-Dmaven.multiModuleProjectDirectory=." -cp ".mvn\wrapper\maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain %*
    exit /b %ERRORLEVEL%
)

echo [ERROR] Could not find Maven or Maven Wrapper!
exit /b 1
