import os
import argparse

import mx
import mx_gate
import mx_unittest


PACKAGE_NAME = 'de.hpi.swa.graal.squeak'
BASE_VM_ARGS = [
    '-Xms2G',  # Initial heap size
    '-XX:MetaspaceSize=64M',  # Initial size of Metaspaces
]

_suite = mx.suite('graalsqueak')
_compiler = mx.suite('compiler', fatalIfMissing=False)


def _graal_vm_args(args):
    graal_args = []

    if args.trace_compilation:
        graal_args += [
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
        print 'Sending Graal dumps to igv...'
        graal_args += [
            '-Dgraal.Dump=Metaclass,Truffle,hpi',
            '-Dgraal.DumpOnError=true',
        ]

    if args.low_level:
        graal_args += [
            '-XX:+UnlockDiagnosticVMOptions',
            '-XX:+LogCompilation',
        ]

    if args.deopts:
        graal_args += [
            '-XX:+TraceDeoptimization',
        ]

    if args.print_machine_code:
        graal_args += [
            '-XX:CompileCommand=print,*OptimizedCallTarget.callRoot',
            '-XX:CompileCommand=exclude,*OptimizedCallTarget.callRoot',
        ]

    if not args.background_compilation:
        graal_args += ['-Dgraal.TruffleBackgroundCompilation=false']

    graal_args += [
        '-Djvmci.Compiler=graal',
        '-XX:+UseJVMCICompiler',
    ]
    return graal_args


def _squeak(args, extra_vm_args=None, env=None, jdk=None, **kwargs):
    """run GraalSqueak"""

    env = env if env else os.environ

    vm_args, raw_args = mx.extract_VM_args(
        args, useDoubleDash=True, defaultAllVMArgs=False)

    parser = argparse.ArgumentParser(prog='mx squeak')
    parser.add_argument('-A', '--assertions',
                        help='enable assertion',
                        dest='assertions',
                        action='store_true', default=False)
    parser.add_argument('-B', '--no-background',
                        help='disable background compilation',
                        dest='background_compilation',
                        action='store_false', default=True)
    parser.add_argument('-d', '--disable-interrupts',
                        help='disable interrupt handler',
                        dest='disable_interrupts',
                        action='store_true', default=False)
    parser.add_argument('--gc', action='store_true',
                        help='print garbage collection details')
    parser.add_argument('--igv', action='store_true', help='dump to igv')
    parser.add_argument('-l', '--low-level',
                        help='enable low-level optimization output',
                        dest='low_level', action='store_true', default=False)
    parser.add_argument('--machine-code',
                        help='print machine code',
                        dest='print_machine_code', action='store_true',
                        default=False)
    parser.add_argument('-m', '--method',
                        help='method selector when receiver is provided',
                        dest='method')
    parser.add_argument('-r', '--receiver',
                        help='SmallInteger to be used as receiver',
                        dest='receiver')
    parser.add_argument(
        '-tc', '--trace-compilation', help='trace Truffle compilation',
        dest='trace_compilation', action='store_true', default=False)
    parser.add_argument(
        '-td', '--trace-deopts', help='trace deoptimizations',
        dest='deopts', action='store_true', default=False)
    parser.add_argument(
        '-ti', '--trace-invalid',
        help='trace assumption invalidation and transfers to interpreter',
        dest='trace_invalidation', action='store_true', default=False)
    parser.add_argument(
        '-ts', '--trace-squeak', help='trace Squeak process switches, ...',
        dest='trace_squeak', action='store_true', default=False)
    parser.add_argument('-v', '--verbose',
                        help='enable verbose output',
                        dest='verbose',
                        action='store_true', default=False)
    parser.add_argument('-w', '--perf-warnings',
                        help='enable performance warnings',
                        dest='perf_warnings',
                        action='store_true', default=False)
    parser.add_argument('image',
                        help='path to Squeak image file',
                        nargs='?')
    parser.add_argument('image_arguments',
                        help='image arguments',
                        nargs=argparse.REMAINDER)
    parsed_args = parser.parse_args(raw_args)

    vm_args = BASE_VM_ARGS
    vm_args += ['-cp', mx.classpath(PACKAGE_NAME)]

    if _compiler:
        vm_args += _graal_vm_args(parsed_args)

    # default: assertion checking is enabled
    if parsed_args.assertions:
        vm_args += ['-ea', '-esa']

    if parsed_args.gc:
        vm_args += ['-XX:+PrintGC', '-XX:+PrintGCDetails']

    if extra_vm_args:
        vm_args += extra_vm_args

    vm_args.append('%s.GraalSqueakMain' % PACKAGE_NAME)

    if parsed_args.receiver and not parsed_args.method:
        parser.error('--method required when --receiver is provided')
    if not parsed_args.receiver and parsed_args.method:
        parser.error('--receiver required when --method is provided')

    squeak_arguments = []
    if parsed_args.disable_interrupts:
        squeak_arguments.append('--disable-interrupts')
    if parsed_args.receiver and parsed_args.method:
        squeak_arguments.extend(['--receiver', parsed_args.receiver,
                                 '--method', parsed_args.method])
    if parsed_args.trace_squeak:
        squeak_arguments.append('--trace')
    if parsed_args.verbose:
        squeak_arguments.append('--verbose')
    if parsed_args.image_arguments:
        squeak_arguments.extend(['--args'] + parsed_args.image_arguments)

    if parsed_args.image:
        squeak_arguments = [parsed_args.image] + squeak_arguments
    else:
        if len(squeak_arguments) > 0:
            parser.error('an image needs to be explicitly provided')

    if not jdk:
        jdk = mx.get_jdk(tag='jvmci' if _compiler else None)

    return mx.run_java(vm_args + squeak_arguments, jdk=jdk, **kwargs)


def _graalsqueak_gate_runner(args, tasks):
    os.environ['MX_GATE'] = 'true'
    unittest_args = BASE_VM_ARGS
    unittest_args += _get_jacoco_agent_args()
    unittest_args += ['--suite', 'graalsqueak']
    with mx_gate.Task('TestGraalSqueak', tasks, tags=['test']) as t:
        if t:
            mx_unittest.unittest(unittest_args)
    with mx_gate.Task('CodeCoverageReport', tasks, tags=['test']) as t:
        if t:
            mx.command_function('jacocoreport')(['--format', 'xml', '.'])


def _get_jacoco_agent_args():
    jacocoagent = mx.library('JACOCOAGENT', True)

    includes = []
    baseExcludes = []
    for p in mx.projects(limit_to_primary=True):
        projsetting = getattr(p, 'jacoco', '')
        if projsetting == 'exclude':
            baseExcludes.append(p.name)
        if projsetting == 'include':
            includes.append(p.name + '.*')

    excludes = [package + '.*' for package in baseExcludes]
    agentOptions = {
                    'append': 'false',
                    'inclbootstrapclasses': 'false',
                    'includes': ':'.join(includes),
                    'excludes': ':'.join(excludes),
                    'destfile': 'jacoco.exec'
    }
    return ['-javaagent:' + jacocoagent.get_path(True) + '=' +
            ','.join([k + '=' + v for k, v in agentOptions.items()])]


mx.update_commands(_suite, {
    'squeak': [_squeak, '[options]'],
})
mx_gate.add_gate_runner(_suite, _graalsqueak_gate_runner)
