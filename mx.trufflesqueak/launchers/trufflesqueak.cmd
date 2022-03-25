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

@REM Increase stack size (`-XX:ThreadStackSize=64M` not working)
set extra_args=--vm.Xss64M
@REM Make ReflectionUtils work
set extra_args=%extra_args% --vm.-add-exports=java.base/jdk.internal.module=ALL-UNNAMED
@REM Make Truffle.getRuntime() accessible for VM introspection
set extra_args=%extra_args% --vm.-add-opens=jdk.internal.vm.compiler/org.graalvm.compiler.truffle.runtime=ALL-UNNAMED
@REM Enable access to HostObject and others
set extra_args=%extra_args% --vm.-add-opens=org.graalvm.truffle/com.oracle.truffle.host=ALL-UNNAMED
@REM Enable access to Truffle's SourceSection (for retrieving sources through interop)
set extra_args=%extra_args% --vm.-add-opens=org.graalvm.truffle/com.oracle.truffle.api.source=ALL-UNNAMED


if "%VERBOSE_GRAALVM_LAUNCHERS%"=="true" echo on

"%tsl_exe%" %extra_args% %*

exit /b %errorlevel%
