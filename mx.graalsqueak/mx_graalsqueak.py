import os
import argparse
import shutil

import mx
import mx_gate
import mx_sdk
import mx_unittest


PACKAGE_NAME = 'de.hpi.swa.graal.squeak'
BASE_VM_ARGS = [
    '-Xms2G',  # Initial heap size
    '-XX:MetaspaceSize=64M',  # Initial size of Metaspaces
]
SVM_BINARY = 'graalsqueak-svm'
SVM_TARGET = os.path.join('bin', SVM_BINARY)

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

    if args.trace_inlining:
        graal_args += [
            '-Dgraal.TraceTruffleInlining=true',
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
        graal_args += ['-XX:+TraceDeoptimization']

    if args.print_machine_code:
        graal_args += [
            '-XX:CompileCommand=print,*OptimizedCallTarget.callRoot',
            '-XX:CompileCommand=exclude,*OptimizedCallTarget.callRoot',
        ]

    if not args.background_compilation:
        graal_args += ['-Dgraal.TruffleBackgroundCompilation=false']

    if args.force_compilation:
        graal_args += ['-Dgraal.TruffleCompileImmediately=true']

    if args.print_graal_options:
        graal_args += ['-XX:+JVMCIPrintProperties']

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
    parser.add_argument('--cpusampler', help='enable CPU sampling',
                        dest='cpusampler', action='store_true', default=False)
    parser.add_argument('--cputracer', help='enable CPU tracing',
                        dest='cputracer', action='store_true', default=False)
    parser.add_argument('-d', '--disable-interrupts',
                        help='disable interrupt handler',
                        dest='disable_interrupts',
                        action='store_true', default=False)
    parser.add_argument(
        '-fc', '--force-compilation',
        help='compile immediately to test Truffle compiler',
        dest='force_compilation', action='store_true', default=False)
    parser.add_argument('--gc', action='store_true',
                        help='print garbage collection details')
    parser.add_argument('--graal-options', help='print Graal options',
                        dest='print_graal_options', action='store_true',
                        default=False)
    parser.add_argument('--igv', action='store_true', help='dump to igv')
    parser.add_argument('--inspect', help='enable Chrome inspector',
                        dest='inspect', action='store_true', default=False)
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
    parser.add_argument('--print-defaults', help='print VM defaults',
                        dest='print_defaults', action='store_true',
                        default=False)
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
        '-tin', '--trace-inlining',
        help='print information for inlining for each compilation.',
        dest='trace_inlining', action='store_true', default=False)
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

    if parsed_args.print_defaults:
        vm_args += ['-XX:+PrintFlagsFinal']

    if extra_vm_args:
        vm_args += extra_vm_args

    vm_args.append('%s.GraalSqueakLauncher' % PACKAGE_NAME)

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
    if parsed_args.cpusampler:
        squeak_arguments.append('--cpusampler')
    if parsed_args.cputracer:
        squeak_arguments.append('--cputracer')
    if parsed_args.inspect:
        squeak_arguments.append('--inspect')

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

    supports_coverage = os.environ.get('JDK') == 'jdk8'  # see `.travis.yml`
    if supports_coverage:
        unittest_args += _get_jacoco_agent_args()
    unittest_args += ['--suite', 'graalsqueak']
    with mx_gate.Task('TestGraalSqueak', tasks, tags=['test']) as t:
        if t:
            mx_unittest.unittest(unittest_args)

    if supports_coverage:
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


def _squeak_svm(args):
    """build GraalSqueak with SubstrateVM"""
    mx.run_mx(
        ['--dynamicimports', '/substratevm,/vm', 'build', '--dependencies',
         '%s.image' % SVM_BINARY],
        nonZeroIsFatal=True
    )
    shutil.copy(_get_svm_binary_from_graalvm(), _get_svm_binary())
    print 'GraalSqueak binary now available at "%s".' % _get_svm_binary()


def _get_svm_binary():
    return os.path.join(_suite.dir, SVM_TARGET)


def _get_svm_binary_from_graalvm():
    vmdir = os.path.join(mx.suite('truffle').dir, '..', 'vm')
    return os.path.join(
        vmdir, 'mxbuild', '-'.join([mx.get_os(), mx.get_arch()]),
        '%s.image' % SVM_BINARY, SVM_BINARY)


_svmsuite = mx.suite('substratevm', fatalIfMissing=False)
if _svmsuite:
    _svmsuite.extensions.flag_suitename_map['squeak'] = (
        'graalsqueak',
        ['GRAALSQUEAK', 'GRAALSQUEAK-CONFIG', 'GRAALSQUEAK-LAUNCHER'], [])

mx_sdk.register_graalvm_component(mx_sdk.GraalVmLanguage(
    suite=_suite,
    name='GraalSqueak',
    short_name='sq',
    dir_name='squeak',
    license_files=[],
    third_party_license_files=[],
    truffle_jars=[
        'graalsqueak:GRAALSQUEAK',
        'graalsqueak:GRAALSQUEAK-CONFIG',
    ],
    support_distributions=[
        'graalsqueak:GRAALSQUEAK_GRAALVM_SUPPORT',
    ],
    launcher_configs=[
        mx_sdk.LanguageLauncherConfig(
            destination=SVM_TARGET,
            jar_distributions=[
                'graalsqueak:GRAALSQUEAK-LAUNCHER',
                'graalsqueak:GRAALSQUEAK-CONFIG',
            ],
            main_class='de.hpi.swa.graal.squeak.launcher.GraalSqueakLauncher',
            build_args=[
                '--language:squeak',
            ]
        )
    ],
))


mx.update_commands(_suite, {
    'squeak': [_squeak, '[options]'],
    'squeak-svm': [_squeak_svm, ''],
})

mx_gate.add_gate_runner(_suite, _graalsqueak_gate_runner)
