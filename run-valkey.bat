@echo off
chcp 65001 >nul
echo ========================================================
echo   KHOI DONG VALKEY VA VALKEY ADMIN CHO MINI MARKET
echo ========================================================
echo.

docker compose up -d

echo.
echo ========================================================
echo   TRANG THAI CONG VA DICH VU:
echo   - Valkey Database : localhost:6379 (CLI/Spring Boot)
echo   - Valkey Admin UI : http://localhost:8085
echo   - Dang nhap Admin : admin / admin
echo ========================================================
echo.
pause
