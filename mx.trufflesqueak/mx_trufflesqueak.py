import os
import argparse

import mx

import mx_gate

import mx_unittest


PACKAGE_NAME = 'de.hpi.swa.trufflesqueak'

_suite = mx.suite('trufflesqueak')
_compiler = mx.suite("compiler", fatalIfMissing=False)
_images = os.path.join(_suite.dir, "images")

os.environ["TRUFFLESQUEAK_ROOT"] = _suite.dir


def _graal_vm_args(args, jdk):
    graal_args = [
        '-Dgraal.TraceTruffleCompilation=true',
    ]

    if args.perf_warnings:
        graal_args += [
            '-Dgraal.TruffleCompilationExceptionsAreFatal=true',
            '-Dgraal.TraceTrufflePerformanceWarnings=true',
            '-Dgraal.TraceTruffleCompilationDetails=true',
            '-Dgraal.TraceTruffleExpansionSource=true']

    if args.trace_invalidation:
        graal_args += [
            '-Dgraal.TraceTruffleTransferToInterpreter=true',
            '-Dgraal.TraceTruffleAssumptions=true',
        ]

    if args.igv:
        graal_args += [
            '-Dgraal.Dump=Metaclass,Truffle,hpi',
        ]

    if args.low_level:
        graal_args += [
            '-XX:+UnlockDiagnosticVMOptions',
            '-XX:+LogCompilation',
            '-XX:+TraceDeoptimization',
        ]

    if not args.background_compilation:
        graal_args += ['-Dgraal.TruffleBackgroundCompilation=false']

    graal_path = mx.classpath('compiler:GRAAL', jdk=jdk)
    graal_args += [
        '-XX:+UseJVMCICompiler',
        '-Djvmci.Compiler=graal',
        '-Djvmci.class.path.append=%s' % graal_path
    ]
    return graal_args


def _squeak(args, extra_vm_args=None, env=None, jdk=None, **kwargs):
    env = env if env else os.environ

    vm_args, raw_args = mx.extract_VM_args(
        args, useDoubleDash=False, defaultAllVMArgs=False)

    parser = argparse.ArgumentParser(prog='mx squeak')
    parser.add_argument('-A', '--enable-assertions',
                        help='Enable assertion',
                        dest='assertions',
                        action='store_true', default=False)
    parser.add_argument('-B', '--no-background',
                        help='disable background compilation',
                        dest='background_compilation',
                        action='store_false', default=True)
    parser.add_argument('--igv', action='store_true', help='Dump to igv')
    parser.add_argument('-l', '--low-level',
                        help='enable low-level optimization output',
                        dest='low_level', action='store_true', default=False)
    parser.add_argument('-ti', '--trace-invalidation',
                        help='trace assumption invalidation and transfers to interpreter',
                        dest='trace_invalidation',
                        action='store_true', default=False)
    parser.add_argument('-w', '--perf-warnings',
                        help='enable performance warnings',
                        dest='perf_warnings',
                        action='store_true', default=False)
    parser.add_argument('squeak_args', nargs=argparse.REMAINDER)
    args = parser.parse_args(raw_args)

    vm_args = ['-cp', mx.classpath(PACKAGE_NAME)]

    if not jdk:
        jdk = mx.get_jdk(tag='jvmci')

    if _compiler:
        vm_args.extend(_graal_vm_args(args, jdk))

    # default: assertion checking is enabled
    if args.assertions:
        vm_args.extend(['-ea', '-esa'])

    if extra_vm_args:
        vm_args.extend(extra_vm_args)

    vm_args.append("de.hpi.swa.trufflesqueak.TruffleSqueakMain")
    return mx.run_java(vm_args + args.squeak_args, jdk=jdk, **kwargs)


def _trufflesqueak_gate_runner(args, tasks):
    argsPrefix = ['--color']
    argsPrefix.append('%s.test' % PACKAGE_NAME)
    print argsPrefix
    with mx_gate.Task("TestTruffleSqueak", tasks, tags=['trufflesqueak']) as t:
        if t:
            mx_unittest.unittest(argsPrefix)

mx.update_commands(_suite, {
    'squeak': [_squeak, '[Squeak args|@VM options]'],
})
mx_gate.add_gate_runner(_suite, _trufflesqueak_gate_runner)
