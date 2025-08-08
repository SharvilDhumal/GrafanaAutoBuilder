@echo off
echo Fixing Java Language Server Issues...
echo.

echo 1. Cleaning Maven project...
cd src\Backend
call mvn clean
echo.

echo 2. Downloading dependencies...
call mvn dependency:resolve
echo.

echo 3. Compiling project...
call mvn compile
echo.

echo 4. Running checkstyle...
call mvn checkstyle:check
echo.

echo 5. Creating target directory structure...
if not exist "target\classes" mkdir target\classes
if not exist "target\test-classes" mkdir target\test-classes
echo.

echo 6. Copying resources...
call mvn resources:resources
echo.

echo Java project setup complete!
echo.
echo Please restart VS Code and reload the window to apply changes.
echo You can do this by pressing Ctrl+Shift+P and typing "Developer: Reload Window"
echo.
pause
