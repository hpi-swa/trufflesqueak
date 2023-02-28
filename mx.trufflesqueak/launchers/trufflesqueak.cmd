@echo off

setlocal enabledelayedexpansion

echo %* | findstr = >nul && (
  echo Warning: the '=' character in program arguments is not fully supported.
  echo Make sure that command line arguments using it are wrapped in double quotes.
  echo Example:
  echo "--vm.Dfoo=bar"
  echo.
)

set "tsl_exe=%~dp0trufflesqueak-launcher.exe"
if not exist "%tsl_exe%" (
  echo Error: Cannot find '%TSL_EXE%'
  exit /b 1
)

@REM Set inital heap size
set extra_args=--vm.Xms512M
@REM Increase stack size
set extra_args=%extra_args% --vm.Xss16M

@REM Default to polyglot launcher in JVM mode
set extra_args=%extra_args% --jvm --polyglot
@REM Make ReflectionUtils work
set extra_args=%extra_args% --vm.-add-exports=java.base/jdk.internal.module=ALL-UNNAMED

if "%VERBOSE_GRAALVM_LAUNCHERS%"=="true" echo on

"%tsl_exe%" %extra_args% %*

exit /b %errorlevel%
