@echo off
title Grant-X — Startup Launcher
color 0A

echo.
echo ============================================================
echo   GRANT-X — Student Innovation Grant and Patent Filing Portal
echo   AIHT CSBS-G11 — Local Development Launcher
echo ============================================================
echo.

REM === CHECK JAVA ===
java -version >nul 2>&1
if errorlevel 1 (
    color 0C
    echo [ERROR] Java is NOT installed or not in PATH.
    echo Please install Java 17 or higher from https://adoptium.net/
    echo.
    pause
    exit /b 1
)

echo [OK] Java found:
java -version 2>&1 | findstr /i "version"

REM === CHECK MAVEN ===
mvn -version >nul 2>&1
if errorlevel 1 (
    color 0C
    echo [ERROR] Maven is NOT installed or not in PATH.
    echo Please install Maven from https://maven.apache.org/
    echo.
    pause
    exit /b 1
)

echo [OK] Maven found:
mvn -version 2>&1 | findstr /i "apache maven"

REM === CHECK MYSQL ===
echo.
echo [INFO] Checking MySQL connection...
mysql -u root -padmin -e "SELECT 1;" >nul 2>&1
if errorlevel 1 (
    color 0E
    echo [WARNING] Could not verify MySQL connection.
    echo Make sure MySQL 8.0 is running on port 3306.
    echo Default credentials: root / (no password)
    echo If you have a password set, edit: backend\src\main\resources\application.properties
    echo.
)

REM === CREATE DATABASE IF NOT EXISTS ===
echo [INFO] Creating database 'grantx_db' if it doesn't exist...
mysql -u root -padmin -e "CREATE DATABASE IF NOT EXISTS grantx_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>nul
if errorlevel 1 (
    echo [WARNING] Could not auto-create database. Please create it manually:
    echo   mysql -u root -p
    echo   CREATE DATABASE grantx_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
)

REM === BUILD BACKEND ===
echo.
echo ============================================================
echo [INFO] Building backend...
echo ============================================================
cd backend
mvn clean package -DskipTests -q
if errorlevel 1 (
    color 0C
    echo.
    echo [ERROR] Build failed! Check the output above for errors.
    cd ..
    pause
    exit /b 1
)
echo [OK] Backend built successfully.
cd ..

REM === START BACKEND ===
echo.
echo ============================================================
echo [INFO] Starting Grant-X backend on port 8080...
echo ============================================================
echo.

start "Grant-X Backend" cmd /k "cd /d %~dp0backend && java -jar target\grant-x-1.0.0.jar && pause"

echo [INFO] Waiting for backend to start (15 seconds)...
timeout /t 15 /nobreak >nul

REM === OPEN FRONTEND ===
echo.
echo ============================================================
echo [INFO] Opening Grant-X Frontend in browser...
echo ============================================================
echo.

set FRONTEND_PATH=%~dp0frontend\index.html
start "" "%FRONTEND_PATH%"

REM === DONE ===
echo.
echo ============================================================
echo   GRANT-X IS NOW RUNNING!
echo ============================================================
echo.
echo   Backend API:    http://localhost:8080
echo   Frontend:       Opened in your browser
echo   API Health:     http://localhost:8080/api/auth/login
echo.
echo   === DEFAULT LOGIN CREDENTIALS ===
echo   Admin:    admin       / admin123
echo   Student:  arjun.cs21  / student123
echo   Faculty:  dr.priya    / faculty123
echo.
echo   Press any key to open the frontend in browser again.
echo   Close the 'Grant-X Backend' window to stop the server.
echo ============================================================
echo.
pause
start "" "%FRONTEND_PATH%"
