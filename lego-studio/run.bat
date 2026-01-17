@echo off
REM Lego Studio 3D - Build and Run Script for Windows
REM Usage: run.bat [build|run|clean]

cd /d "%~dp0"

if "%1"=="" goto run
if "%1"=="build" goto build
if "%1"=="run" goto run
if "%1"=="package" goto package
if "%1"=="clean" goto clean
goto usage

:build
echo Building Lego Studio...
call mvn clean compile -q
echo Build complete!
goto end

:run
echo Building and running Lego Studio...
call mvn clean compile exec:exec -q
goto end

:package
echo Packaging Lego Studio...
call mvn clean package -q
echo JAR created in target/
goto end

:clean
echo Cleaning build files...
call mvn clean -q
echo Clean complete!
goto end

:usage
echo Usage: run.bat [build^|run^|package^|clean]
echo   build   - Compile the project
echo   run     - Build and run (default)
echo   package - Create executable JAR
echo   clean   - Remove build files
goto end

:end
