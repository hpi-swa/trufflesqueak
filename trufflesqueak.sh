#!/usr/bin/env bash

SCRIPT_PATH=`dirname $0`
JAVACMD=${JAVACMD:=./bin/graalvm}

PROGRAM_ARGS=""
JAVA_ARGS="-J:-Dgraal.TruffleCompilationThreshold=10"

for opt in "$@"; do
    case $opt in
	-debug)
	    JAVA_ARGS="$JAVA_ARGS -J:-Xdebug -J:-Xrunjdwp:transport=dt_socket,server=y,address=8000,suspend=y"
	    ;;
	-dump)
	    JAVA_ARGS="$JAVA_ARGS -J:-Dgraal.Dump= -J:-Dgraal.MethodFilter=Truffle.* -J:-Dgraal.TruffleBackgroundCompilation=false -J:-Dgraal.TraceTruffleCompilation=true -J:-Dgraal.TraceTruffleCompilationDetails=true"
	    ;;
	-disassemble)
	    JAVA_ARGS="$JAVA_ARGS -J:-XX:CompileCommand=print,*OptimizedCallTarget.callRoot -J:-XX:CompileCommand=exclude,*OptimizedCallTarget.callRoot -J:-Dgraal.TruffleBackgroundCompilation=false -J:-Dgraal.TraceTruffleCompilation=true -J:-Dgraal.TraceTruffleCompilationDetails=true"
	    ;;
	-int)
	    JAVACMD="./bin/java"
	    JAVA_ARGS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=8000,suspend=y"
	    ;;
	-J*)
	    JAVA_ARGS="$JAVA_ARGS $opt" ;;
	*)
	    PROGRAM_ARGS="$PROGRAM_ARGS $opt" ;;
    esac
done

echo "$PROGRAM_ARGS"

$JAVACMD $JAVA_ARGS -cp trufflesqueak.jar de.hpi.swa.trufflesqueak.TruffleSqueakMain $PROGRAM_ARGS
