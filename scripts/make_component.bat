:: #
:: # Copyright (c) 2019-2019 Software Architecture Group, Hasso Plattner Institute
:: #
:: # Licensed under the MIT License.
:: #
@echo off
setlocal enabledelayedexpansion

set _EXITCODE=0

call :env
if not %_EXITCODE%==0 goto end

set _LANGUAGE_ID=smalltalk
set _GRAALVM_VERSION=19.2.1
set _OS_NAME=windows
set _OS_NAME_FOR_GU=%_OS_NAME%
set _OS_ARCH_FOR_GU=amd64
for /f "usebackq" %%i in (`"%_GIT_CMD%" describe --tags`) do set _GIT_DESCRIPTION=%%i

for %%f in ("%~dp0") do set "_BASE_DIR=%%~f"
set "_COMPONENT_DIR=%TEMP%\component_temp_dir"
for %%f in ("%~dp0..") do set "_GRAALSQUEAK_DIR=%%~f"
set "_GRAALSQUEAK_JAR=%_GRAALSQUEAK_DIR%\graalsqueak.jar"
set "_LANGUAGE_PATH=%_COMPONENT_DIR%\jre\languages\%_LANGUAGE_ID%"
set "_LIB_GRAALVM_PATH=%_COMPONENT_DIR%\jre\lib\graalvm"
set "_MANIFEST=%_COMPONENT_DIR%\META-INF\MANIFEST.MF"
set "_RELEASE_FILE=%_LANGUAGE_PATH%\release"
set "_TARGET_JAR=%_GRAALSQUEAK_DIR%\graalsqueak-installable-%_OS_NAME%-%_OS_ARCH_FOR_GU%-%_GIT_DESCRIPTION%-for-GraalVM-%_GRAALVM_VERSION%.jar"
set _TEMPLATE_LAUNCHER=template.graalsqueak.sh
set _TEMPLATE_WIN_LAUNCHER=template.graalsqueak.cmd

if exist "%_COMPONENT_DIR%\" (
    set /p _USER_INPUT='%_COMPONENT_DIR%' already exists. Do you want to remove it? ^(y/N^) 
    if /i not "!_USER_INPUT!"=="y" (
        rem we abort script execution
        goto end
    )
    rmdir /s /q "%_COMPONENT_DIR%"
)

if not exist "%_GRAALSQUEAK_JAR%" (
    echo Could not find '%_GRAALSQUEAK_JAR%'. Did you run 'mx build'?
    set _EXITCODE=1
    goto end
)

mkdir "%_LANGUAGE_PATH%" "%_LANGUAGE_PATH%\bin" "%_LIB_GRAALVM_PATH%"
copy /y "%_GRAALSQUEAK_JAR%" "%_LANGUAGE_PATH%" 1>NUL
copy /y "%_GRAALSQUEAK_DIR%\graalsqueak-shared.jar" "%_LANGUAGE_PATH%" 1>NUL
copy /y "%_BASE_DIR%\%_TEMPLATE_LAUNCHER%" "%_LANGUAGE_PATH%\bin\graalsqueak" 1>NUL
copy /y "%_BASE_DIR%\%_TEMPLATE_WIN_LAUNCHER%" "%_LANGUAGE_PATH%\bin\graalsqueak.cmd" 1>NUL
copy /y "%_GRAALSQUEAK_DIR%\graalsqueak-launcher.jar" "%_LIB_GRAALVM_PATH%" 1>NUL
copy /y "%_GRAALSQUEAK_DIR%\LICENSE" "%_COMPONENT_DIR%\LICENSE_GRAALSQUEAK.txt" 1>NUL

mkdir "%_COMPONENT_DIR%\META-INF"

echo Bundle-Name: GraalSqueak> "%_MANIFEST%"
echo Bundle-Symbolic-Name: de.hpi.swa.graal.squeak>> "%_MANIFEST%"
echo Bundle-Version: %_GRAALVM_VERSION%>> "%_MANIFEST%"
echo Bundle-RequireCapability: org.graalvm; filter:="(&(graalvm_version=%_GRAALVM_VERSION%)(os_name=%_OS_NAME_FOR_GU%)(os_arch=%_OS_ARCH_FOR_GU%))">> "%_MANIFEST%"
echo x-GraalVM-Polyglot-Part: True>> "%_MANIFEST%"

for /f "usebackq" %%i in (`%_GIT_CMD% rev-parse HEAD`) do set _GIT_HASH=%%i
for /f "usebackq" %%i in (`%_GIT_CMD% rev-parse --abbrev-ref HEAD`) do set _GIT_BRANCH_NAME=%%i
for /f "usebackq delims=" %%i in (`%_GIT_CMD% config user.name`) do set _GIT_COMMITTER_NAME=%%i
for /f "usebackq" %%i in (`%_GIT_CMD% config user.email`) do set _GIT_COMMITTER_EMAIL=%%i

echo OS_NAME=%_OS_NAME_FOR_GU%> "%_RELEASE_FILE%"
echo OS_ARCH=%_OS_ARCH_FOR_GU%>> "%_RELEASE_FILE%"
echo SOURCE="%_GIT_BRANCH_NAME%:%_GIT_HASH%">> "%_RELEASE_FILE%"
echo COMMIT_INFO={"%_GIT_BRANCH_NAME%": {"commit.committer": "%_GIT_COMMITTER_NAME% <%_GIT_COMMITTER_EMAIL%>", "commit.rev": "%_GIT_HASH%"}}>> "%_RELEASE_FILE%"
echo GRAALVM_VERSION=%_GRAALVM_VERSION%>> "%_RELEASE_FILE%"
rem echo component_catalog=...>> "%_RELEASE_FILE%"

pushd "%_COMPONENT_DIR%"
"%_JAR_CMD%" cfm "%_TARGET_JAR%" META-INF\MANIFEST.MF .
if not %ERRORLEVEL%==0 (
    popd
    echo Failed to create Java archive %_TARGET_JAR% 1>&2
    set _EXITCODE=1
    goto end
)
echo bin/graalsqueak = ../jre/bin/graalsqueak> META-INF\symlinks
echo jre/bin/graalsqueak = ../languages/%_LANGUAGE_ID%/bin/graalsqueak>> META-INF\symlinks
"%_JAR_CMD%" uf "%_TARGET_JAR%" META-INF\symlinks
if not %ERRORLEVEL%==0 (
    popd
    echo Failed to update Java archive %_TARGET_JAR% 1>&2
    set _EXITCODE=1
    goto end
)
echo jre\languages\%_LANGUAGE_ID%\bin\graalsqueak = rwxrwxr-x> META-INF\permissions
"%_JAR_CMD%" uf "%_TARGET_JAR%" META-INF\permissions
if not %ERRORLEVEL%==0 (
    popd
    echo Failed to update Java archive %_TARGET_JAR% 1>&2
    set _EXITCODE=1
    goto end
)
popd
rmdir /s /q "%_COMPONENT_DIR%"

echo SUCCESS^^! The component is located at '%_TARGET_JAR%'.
goto end

:: ###########################################################################
:: ## Subroutines

rem output parameter(s): _GIT_CMD, _JAR_CMD
:env
where /q git.exe
if not %ERRORLEVEL%==0 (
    echo Could not find executable git.exe 1>&2
    set _EXITCODE=1
    goto :eof
)
set _GIT_CMD=git.exe

where /q jar.exe
if not %ERRORLEVEL%==0 (
    echo Could not find executable jar.exe 1>&2
    set _EXITCODE=1
    goto :eof
)
set _JAR_CMD=jar.exe
goto :eof

:: ###########################################################################
:: ## Cleanups

:end
exit /b %_EXITCODE%
endlocal