#!/usr/bin/env bash
set -e

readonly VERSION="1.0.0-rc16"

readonly MAIN_CLASS="de.hpi.swa.graal.squeak.launcher.GraalSqueakLauncher"
readonly SCRIPT_HOME="$(cd "$(dirname "$0")" && pwd -P)"
readonly LIB_DIR="${SCRIPT_HOME}/../jre/lib"

#######################################################################
# Prepare to run as component for GraalVM.                            #
#######################################################################
GRAALVM_VERSION=$(grep "GRAALVM_VERSION" "$SCRIPT_HOME/../release" 2> /dev/null)
if [[ "${GRAALVM_VERSION}" == "" ]]; then
    echo "Could not determine \$GRAALVM_VERSION"
    exit 1
fi
readonly BOOT_PATH="${LIB_DIR}/boot/graal-sdk.jar:${LIB_DIR}/truffle/truffle-api.jar"
readonly LAUNCHER_PATH="${LIB_DIR}/graalvm/launcher-common.jar:${LIB_DIR}/graalvm/graalsqueak-launcher.jar"
readonly JAVACMD="$SCRIPT_HOME/java"
GRAALVM_VERSION=$(echo "${GRAALVM_VERSION}" | awk 'BEGIN {FS="="} {print $2}')
if [[ "${GRAALVM_VERSION}" != "${VERSION}" ]]; then
    echo "Installed in wrong version of GraalVM. Expected: ${VERSION}, found ${GRAALVM_VERSION}"
    exit 1
fi

#######################################################################
# Parse arguments, prepare Java command, and execute.                 #
#######################################################################
PROGRAM_ARGS="--polyglot"
JAVA_ARGS="-Xss64M -Xms2G -XX:MetaspaceSize=48M"

for opt in "$@"; do
  case $opt in
    -debug)
      JAVA_ARGS="${JAVA_ARGS} -Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=8000,suspend=y" ;;
    -dump)
      JAVA_ARGS="${JAVA_ARGS} -Dgraal.Dump=Truffle:1 -Dgraal.TruffleBackgroundCompilation=false -Dgraal.TraceTruffleCompilation=true -Dgraal.TraceTruffleCompilationDetails=true" ;;
    -disassemble)
      JAVA_ARGS="${JAVA_ARGS} -XX:CompileCommand=print,*OptimizedCallTarget.callRoot -XX:CompileCommand=exclude,*OptimizedCallTarget.callRoot -Dgraal.TruffleBackgroundCompilation=false -Dgraal.TraceTruffleCompilation=true -Dgraal.TraceTruffleCompilationDetails=true" ;;
    -J*)
      opt=${opt:2}
      JAVA_ARGS="${JAVA_ARGS} $opt" ;;
    *)
      PROGRAM_ARGS="${PROGRAM_ARGS} ${opt}" ;;
  esac
done
$JAVACMD $JAVA_ARGS -Xbootclasspath/a:$BOOT_PATH -cp $LAUNCHER_PATH $MAIN_CLASS $PROGRAM_ARGS
