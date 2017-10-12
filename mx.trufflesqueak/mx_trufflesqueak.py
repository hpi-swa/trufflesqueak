from argparse import ArgumentParser
import re
import os
import sys
import subprocess
import urllib2
import mx
import mx_benchmark
import mx_gate
from mx_gate import Task
from mx_unittest import unittest


def squeak(args, extra_vm_args=None, env=None, jdk=None, **kwargs):
    if not env:
        env = os.environ

    check_vm_env = env.get('GRAALPYTHON_MUST_USE_GRAAL', False)
    if check_vm_env:
        if check_vm_env == '1':
            check_vm(must_be_jvmci=True)
        elif check_vm_env == '0':
            check_vm()

    vm_args, squeak_args = mx.extract_VM_args(args, useDoubleDash=False, defaultAllVMArgs=False)

    classpath = ["de.hpi.swa.trufflesqueak"]
    if mx.suite("tools-enterprise", fatalIfMissing=False):
        classpath.append("tools-enterprise:CHROMEINSPECTOR")
    vm_args = ['-cp', mx.classpath(classpath)]

    if not jdk:
        jdk = mx.get_jdk()

    vm_args += [
        # '-XX:+UseJVMCICompiler',
        # '-Djvmci.Compiler=graal',
        # '-Dgraal.TraceTruffleCompilation=true',
        # '-Dgraal.Dump=',
        # '-Dgraal.MethodFilter=Truffle.*',
        # '-XX:CompileCommand=print,*OptimizedCallTarget.callRoot',
        # '-XX:CompileCommand=exclude,*OptimizedCallTarget.callRoot',
        # '-Dgraal.TruffleBackgroundCompilation=false',
        # '-Dgraal.TruffleCompileImmediately=true',
        # '-Dgraal.TraceTrufflePerformanceWarnings=true',
        # '-Dgraal.TruffleCompilationExceptionsArePrinted=true',
    ]

    # default: assertion checking is enabled
    if extra_vm_args is None or '-da' not in extra_vm_args:
        vm_args += ['-ea', '-esa']

    if extra_vm_args:
        vm_args += extra_vm_args

    vm_args.append("de.hpi.swa.trufflesqueak.TruffleSqueakMain")
    return mx.run_java(vm_args + squeak_args, jdk=jdk, **kwargs)


mx.update_commands(mx.suite('trufflesqueak'), {
    'squeak': [squeak, '[Squeak args|@VM options]'],
})
