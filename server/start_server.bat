@echo off
echo Starting GPS Walking App Server...
echo.
echo If Node.js is not installed, please install it from https://nodejs.org/
echo.
node --version
if %errorlevel% neq 0 (
    echo Node.js is not installed or not in PATH
    echo Please install Node.js and try again
    pause
    exit /b 1
)

echo.
echo Installing dependencies...
npm install

echo.
echo Starting server...
npm start

pause
