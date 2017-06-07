#!/usr/bin/env bash

SCRIPT_PATH=`dirname $0`
JAVACMD=${JAVACMD:=./bin/graalvm}
PROGRAM_ARGS=""
CLASSPATH=$SCRIPT_PATH/trufflesqueak.jar

if [ -z $JAVACMD ] || [ ! -x $JAVACMD ]; then
    JAVACMD=java
    CLASSPATH=$SCRIPT_PATH/../truffle/truffle/mxbuild/dists/truffle-api.jar:$CLASSPATH
    PROGRAM_ARGS="$@"
else
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
fi

$JAVACMD $JAVA_ARGS -cp $CLASSPATH de.hpi.swa.trufflesqueak.TruffleSqueakMain $PROGRAM_ARGS
