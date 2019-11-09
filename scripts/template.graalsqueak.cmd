:: #
:: # Copyright (c) 2019-2019 Software Architecture Group, Hasso Plattner Institute
:: #
:: # Licensed under the MIT License.
:: #
@echo off
setlocal enabledelayedexpansion

set _VERSION=19.2.1

set _MAIN_CLASS=de.hpi.swa.graal.squeak.launcher.GraalSqueakLauncher
for %%f in ("%~dp0..\..\..\..") do set "_ROOT_DIR=%%~f"
set "_LIB_DIR=%_ROOT_DIR%\jre\lib"

:: #######################################################################
:: # Prepare to run as component for GraalVM.                            #
:: #######################################################################
set _GRAALVM_VERSION=
if exist "%_ROOT_DIR%\release" for /f %%i in (%_ROOT_DIR%\release) do (
    set __LINE=%%i
    if "!__LINE:~0,15!"=="GRAALVM_VERSION" set _GRAALVM_VERSION=!__LINE:~16!
)
if not defined _GRAALVM_VERSION (
    echo Could not determine Graal version 1>&2
    exit /b 1
) else if not "%_GRAALVM_VERSION%"=="%_VERSION%" (
    echo Installed in wrong version of GraalVM. Expected: %_VERSION%, found %_GRAALVM_VERSION% 1>&2
    exit /b 1
)
set "_BOOT_PATH=%_LIB_DIR%\boot\graal-sdk.jar;%_LIB_DIR%\truffle\truffle-api.jar"
set "_LAUNCHER_PATH=%_LIB_DIR%\graalvm\launcher-common.jar;%_LIB_DIR%\graalvm\graalsqueak-launcher.jar"
set "_JAVACMD=%_ROOT_DIR%\jre\bin\java.exe"

:: #######################################################################
:: # Parse arguments, prepare Java command, and execute.                 #
:: #######################################################################
set _PROGRAM_ARGS=--polyglot
set _JAVA_ARGS=-Xss64M -XX:OldSize=256M -XX:NewSize=1G -XX:MetaspaceSize=32M

for %%i in (%*) do (
    set "__OPT=%%~i"
    if /i "!__OPT!"=="" (
        rem ignore empty option
    ) else if /i "!__OPT!"=="-debug" (
        set _JAVA_ARGS=!_JAVA_ARGS! -Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=8000,suspend=y
    ) else if /i "!__OPT!"=="-dump" (
        set _JAVA_ARGS=!_JAVA_ARGS! -Dgraal.Dump=Truffle:1 -Dgraal.TruffleBackgroundCompilation=false -Dgraal.TraceTruffleCompilation=true -Dgraal.TraceTruffleCompilationDetails=true
    ) else if  /i "!__OPT!"=="-disassemble" (
        set _JAVA_ARGS=!_JAVA_ARGS! -XX:CompileCommand=print,*OptimizedCallTarget.callRoot -XX:CompileCommand=exclude,*OptimizedCallTarget.callRoot -Dgraal.TruffleBackgroundCompilation=false -Dgraal.TraceTruffleCompilation=true -Dgraal.TraceTruffleCompilationDetails=true
    ) else if /i "!__OPT:~0,2!"=="-J" (
        set _JAVA_ARGS=!_JAVA_ARGS! !__OPT:~2!
    ) else (
        set _PROGRAM_ARGS=!_PROGRAM_ARGS! "!__OPT!"
    )
)
"%_JAVACMD%" %_JAVA_ARGS% -Xbootclasspath/a:%_BOOT_PATH% -cp %_LAUNCHER_PATH% %_MAIN_CLASS% %_PROGRAM_ARGS%
exit /b %ERRORLEVEL%
endlocal
