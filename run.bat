@echo off
chcp 65001 > nul 2>&1
echo ========================================================
echo   KHOI CHAY HE THONG SUPERMARKET MINI-MART (BACKEND)
echo ========================================================

if exist "C:\Program Files\Java\jdk-21.0.11\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.11"
) else if exist "C:\Program Files\Java\jdk-17\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-17"
)

if defined JAVA_HOME (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

set "JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8"

echo [1/2] Kiem tra file jar san sang...
if exist "target\gorocery-shop-1.0.0.jar" (
    echo [2/2] Khoi chay truc tiep tu goi JAR...
    java -jar target\gorocery-shop-1.0.0.jar
) else (
    echo [2/2] Khoi chay thong qua Maven Wrapper...
    call .\mvnw.cmd spring-boot:run
)
pause
