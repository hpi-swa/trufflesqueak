#
# Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
#
# Licensed under the MIT License.
#

from __future__ import print_function

import os
import argparse
import shutil

import mx
import mx_gate
import mx_sdk
import mx_truffle
import mx_unittest


LANGUAGE_ID = 'smalltalk'
PACKAGE_NAME = 'de.hpi.swa.graal.squeak'
BASE_DIR = os.path.dirname(os.path.dirname(os.path.realpath(__file__)))
BASE_VM_ARGS = [
    # RUNTIME
    '-Xss64M',  # Increase stack size (`-XX:ThreadStackSize=64M` not working)

    # GARBAGE COLLECTOR (optimized for GraalSqueak image)
    '-XX:OldSize=256M',         # Initial tenured generation size
    '-XX:NewSize=1G',           # Initial new generation size
    '-XX:MetaspaceSize=32M',    # Initial size of Metaspaces
]
BASE_VM_ARGS_TESTING = [
    # RUNTIME
    '-Xss64M',  # Increase stack size (`-XX:ThreadStackSize=64M` not working)

    # GARBAGE COLLECTOR (optimized for Travis CI)
    '-Xms4G',                   # Initial heap size
    '-XX:MetaspaceSize=32M',    # Initial size of Metaspaces

    # JVMCI
    '-XX:-UseJVMCIClassLoader',
]
SVM_BINARY = 'graalsqueak-svm'
SVM_TARGET = os.path.join('bin', SVM_BINARY)
SVM_TARGET_DIR = os.path.join(BASE_DIR, 'bin')

_suite = mx.suite('graalsqueak')
_compiler = mx.suite('compiler', fatalIfMissing=False)


def _graal_vm_args(args):
    graal_args = []

    if args.trace_compilation:
        graal_args += ['-Dgraal.TraceTruffleCompilation=true']

    if args.truffle_compilation_details:
        graal_args += [
            '-Dgraal.TraceTruffleCompilationDetails=true',
            '-Dgraal.TraceTruffleExpansionSource=true']

    if args.truffle_compilation_polymorphism:
        graal_args += ['-Dgraal.TraceTruffleCompilationPolymorphism=true']

    if args.truffle_compilation_stats:
        graal_args += ['-Dgraal.TruffleCompilationStatistics=true']

    if args.truffle_expansion_histogram:
        graal_args += ['-Dgraal.PrintTruffleExpansionHistogram=true']

    if args.truffle_instrument_boundaries:
        graal_args += [
            '-Dgraal.TruffleInstrumentBoundaries=true',
            '-Dgraal.TruffleInstrumentBoundariesPerInlineSite=true',
        ]

    if args.perf_warnings:
        graal_args += [
            '-Dgraal.TruffleCompilationExceptionsAreFatal=true',
            '-Dgraal.TraceTrufflePerformanceWarnings=true']

    if args.trace_invalidation:
        graal_args += [
            '-Dgraal.TraceTruffleTransferToInterpreter=true',
            '-Dgraal.TraceTruffleAssumptions=true',
        ]

    if args.trace_inlining:
        graal_args += ['-Dgraal.TraceTruffleInlining=true']

    if args.trace_splitting:
        graal_args += [
            # '-Dgraal.TraceTruffleSplitting=true',
            '-Dgraal.TruffleTraceSplittingSummary=true',
        ]

    if args.igv:
        print('Sending Graal dumps to igv...')
        graal_args += [
            '-Dgraal.TruffleBackgroundCompilation=false',
            # Use `Truffle:2` for graphs between each compiler phase
            '-Dgraal.Dump=Truffle:1',
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
        # '-XX:-UseJVMCINativeLibrary',  # to disable libgraal
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
    parser.add_argument('-c', '--code',
                        help='Smalltalk code to be executed in headless mode',
                        dest='code')
    parser.add_argument('--cpusampler', help='enable CPU sampling',
                        dest='cpusampler', action='store_true', default=False)
    parser.add_argument('--cputracer', help='enable CPU tracing',
                        dest='cputracer', action='store_true', default=False)
    parser.add_argument('-d', '--disable-interrupts',
                        help='disable interrupt handler',
                        dest='disable_interrupts',
                        action='store_true', default=False)
    parser.add_argument('-etf', '--enable-transcript-forwarding',
                        help='Forward stdio to Transcript',
                        dest='enable_transcript_forwarding',
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
    parser.add_argument('--headless', help='Run without a display',
                        dest='headless', action='store_true', default=False)
    parser.add_argument('--igv', action='store_true', help='dump to igv')
    parser.add_argument('--inspect', help='enable Chrome inspector',
                        dest='inspect', action='store_true', default=False)
    parser.add_argument('--jdk-ci-time',
                        help='collect timing information for compilation '
                        '(contains `JVMCI-native` when libgraal is used)',
                        dest='jdk_ci_time', action='store_true', default=False)
    parser.add_argument('-l', '--low-level',
                        help='enable low-level optimization output',
                        dest='low_level', action='store_true', default=False)
    parser.add_argument('--log',
                        help='enable TruffleLogger for class, e.g.: '
                        '"%s.model.ArrayObject=FINER"' % PACKAGE_NAME,
                        dest='log')
    parser.add_argument('--machine-code',
                        help='print machine code',
                        dest='print_machine_code', action='store_true',
                        default=False)
    parser.add_argument('--memtracer', help='enable Memory tracing',
                        dest='memtracer', action='store_true', default=False)
    parser.add_argument('--print-defaults', help='print VM defaults',
                        dest='print_defaults', action='store_true',
                        default=False)
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
        help='print information for inlining for each compilation',
        dest='trace_inlining', action='store_true', default=False)
    parser.add_argument(
        '-tio', '--trace-interop',
        help='trace interop errors, ...',
        dest='trace_interop', action='store_true', default=False)
    parser.add_argument(
        '-tif', '--trace-iterate-frames',
        help='trace iterate frames',
        dest='trace_iterate_frames', action='store_true', default=False)
    parser.add_argument(
        '-tpf', '--trace-primitive-failures',
        help='trace primitive failures',
        dest='trace_primitive_failures', action='store_true', default=False)
    parser.add_argument(
        '-tps', '--trace-process-switches',
        help='trace Squeak process switches, ...',
        dest='trace_process_switches', action='store_true', default=False)
    parser.add_argument(
        '-ts', '--trace-splitting',
        help='print splitting summary on shutdown',
        dest='trace_splitting', action='store_true', default=False)
    parser.add_argument(
        '-tcd', '--truffle-compilation-details',
        help='print Truffle compilation details',
        dest='truffle_compilation_details', action='store_true', default=False)
    parser.add_argument(
        '-tcp', '--truffle-compilation-polymorphism',
        help='print all polymorphic and generic nodes after each compilation',
        dest='truffle_compilation_polymorphism', action='store_true',
        default=False)
    parser.add_argument(
        '-tcs', '--truffle-compilation-statistics',
        help='print Truffle compilation statistics at the end of a run',
        dest='truffle_compilation_stats', action='store_true', default=False)
    parser.add_argument(
        '-teh', '--truffle-expansion-histogram',
        help='print a histogram of all expanded Java methods',
        dest='truffle_expansion_histogram', action='store_true', default=False)
    parser.add_argument(
        '-tib', '--truffle-instrument-boundaries',
        help='Instrument Truffle boundaries and output profiling information',
        dest='truffle_instrument_boundaries', action='store_true',
        default=False)
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

    vm_args = BASE_VM_ARGS + [
        '-cp', mx.classpath([PACKAGE_NAME, '%s.launcher' % PACKAGE_NAME]),
    ]

    if _compiler:
        vm_args += _graal_vm_args(parsed_args)

    # default: assertion checking is enabled
    if parsed_args.assertions:
        vm_args += ['-ea', '-esa']

    if parsed_args.gc:
        vm_args += ['-XX:+PrintGC', '-XX:+PrintGCDetails']

    if parsed_args.jdk_ci_time:
        vm_args.append('-XX:+CITime')

    if parsed_args.print_defaults:
        vm_args.append('-XX:+PrintFlagsFinal')

    if extra_vm_args:
        vm_args += extra_vm_args

    if parsed_args.code:
        vm_args.append('-Djava.awt.headless=true')

    vm_args.append('%s.launcher.GraalSqueakLauncher' % PACKAGE_NAME)

    squeak_arguments = []
    if parsed_args.disable_interrupts:
        squeak_arguments.append(
            '--%s.DisableInterruptHandler' % LANGUAGE_ID)
    if parsed_args.headless:
        squeak_arguments.append('--%s.Headless' % LANGUAGE_ID)
    if parsed_args.code:
        squeak_arguments.extend(['--code', parsed_args.code])
    if parsed_args.cpusampler:
        squeak_arguments.append('--cpusampler')
    if parsed_args.cputracer:
        squeak_arguments.append('--cputracer')
    if parsed_args.enable_transcript_forwarding:
        squeak_arguments.append('--enable-transcript-forwarding')
    if parsed_args.inspect:
        squeak_arguments.append('--inspect')
    if parsed_args.trace_interop:
        parsed_args.log = (
            '%s.nodes.plugins.PolyglotPlugin=FINE' % PACKAGE_NAME)
    if parsed_args.trace_iterate_frames:
        parsed_args.log = (
            '%s.util.FrameAccess=FINE' % PACKAGE_NAME)
    if parsed_args.trace_primitive_failures:
        parsed_args.log = (
            '%s.nodes.bytecodes.MiscellaneousBytecodes$CallPrimitiveNode=FINE'
            % PACKAGE_NAME)
    if parsed_args.trace_process_switches:
        parsed_args.log = (
            '%s.nodes.ExecuteTopLevelContextNode=FINE' % PACKAGE_NAME)
    if parsed_args.log:
        split = parsed_args.log.split("=")
        if len(split) != 2:
            mx.abort('Must be in the format de.hpi.swa.graal...Class=LOGLEVEL')
        squeak_arguments.append(
            '--log.%s.%s.level=%s' % (LANGUAGE_ID, split[0], split[1]))
    if parsed_args.memtracer:
        squeak_arguments.extend(['--experimental-options', '--memtracer'])

    squeak_arguments.append('--polyglot')  # enable polyglot mode by default

    if parsed_args.image:
        squeak_arguments.append(parsed_args.image)
    else:
        if len(squeak_arguments) > 1:
            parser.error('an image needs to be explicitly provided')

    if parsed_args.image_arguments:
        squeak_arguments.extend(parsed_args.image_arguments)

    if not jdk:
        jdk = mx.get_jdk(tag='jvmci' if _compiler else None)

    return mx.run_java(vm_args + squeak_arguments, jdk=jdk, **kwargs)


def _graalsqueak_gate_runner(args, tasks):
    os.environ['MX_GATE'] = 'true'
    supports_coverage = os.environ.get('JDK') == 'openjdk8'  # see .travis.yml

    _add_copyright_checks(tasks)
    _add_tck_tests(tasks, supports_coverage)
    _add_unit_tests(tasks, supports_coverage)

    if supports_coverage:
        with mx_gate.Task('Report Code Coverage', tasks, tags=['test']) as t:
            if t:
                mx.command_function('jacocoreport')(['--format', 'xml', '.'])


def _add_copyright_checks(tasks):
    with mx_gate.Task('Check Copyrights',
                      tasks, tags=[mx_gate.Tags.style]) as t:
        if t:
            if mx.checkcopyrights(['--primary']) != 0:
                t.abort('Copyright errors found. Please run "mx '
                        'checkcopyrights --primary -- --fix" to fix them.')


def _add_unit_tests(tasks, supports_coverage):
    with mx_gate.Task('GraalSqueak JUnit and SUnit tests',
                      tasks, tags=['test']) as t:
        if t:
            unittest_args = BASE_VM_ARGS_TESTING[:]
            if supports_coverage:
                unittest_args.extend(_get_jacoco_agent_args())
            unittest_args.extend([
                '--suite', 'graalsqueak', '--very-verbose', '--enable-timing'])

            # Ensure Truffle TCK disabled (workaround needed since GraalVM 19.2.0)
            mx_unittest._config_participants.remove(
                mx_truffle._unittest_config_participant_tck)

            mx_unittest.unittest(unittest_args)


def _add_tck_tests(tasks, supports_coverage):
    with mx_gate.Task('GraalSqueak TCK tests', tasks, tags=['test']) as t:
        if t:
            unittest_args = BASE_VM_ARGS_TESTING[:]
            if supports_coverage:
                unittest_args.extend(_get_jacoco_agent_args())
            test_image = _get_path_to_test_image()
            unittest_args.extend([
                '-Dtck.language=%s' % LANGUAGE_ID,
                '-Dpolyglot.%s.Headless=true' % LANGUAGE_ID,
                '-Dpolyglot.%s.ImagePath=%s' % (LANGUAGE_ID, test_image),
                'com.oracle.truffle.tck.tests'])
            mx_unittest.unittest(unittest_args)


def _get_jacoco_agent_args():
    # Modified version of mx_gate.get_jacoco_agent_args()
    agentOptions = {
        'append': 'true',
        'includes': '%s.*' % PACKAGE_NAME,
        'destfile': mx_gate.JACOCO_EXEC,
    }
    return ['-javaagent:' + mx_gate.get_jacoco_agent_path(True) + '=' +
            ','.join([k + '=' + v for k, v in agentOptions.items()])]


def _get_path_to_test_image():
    images_dir = os.path.join(BASE_DIR, 'images')
    image_64bit = os.path.join(images_dir, 'test-64bit.image')
    if os.path.isfile(image_64bit):
        return image_64bit
    image_32bit = os.path.join(images_dir, 'test-32bit.image')
    if os.path.isfile(image_32bit):
        return image_32bit
    mx.abort('Unable to locate test image.')


def _squeak_svm(args):
    """build GraalSqueak with SubstrateVM"""
    mx.run_mx(
        ['--dynamicimports', '/substratevm,/vm', 'build', '--dependencies',
         '%s.image' % SVM_BINARY],
        nonZeroIsFatal=True
    )
    if not os.path.isdir(SVM_TARGET_DIR):
        os.mkdir(SVM_TARGET_DIR)
    shutil.copy(_get_svm_binary_from_graalvm(), _get_svm_binary())
    print('GraalSqueak binary now available at "%s".' % _get_svm_binary())


def _get_svm_binary():
    return os.path.join(_suite.dir, SVM_TARGET)


def _get_svm_binary_from_graalvm():
    vmdir = os.path.join(mx.suite('truffle').dir, '..', 'vm')
    return os.path.join(
        vmdir, 'mxbuild', '-'.join([mx.get_os(), mx.get_arch()]),
        '%s.image' % SVM_BINARY, SVM_BINARY)


mx_sdk.register_graalvm_component(mx_sdk.GraalVmLanguage(
    suite=_suite,
    name='GraalSqueak',
    short_name='st',
    dir_name=LANGUAGE_ID,
    license_files=['LICENSE_GRAALSQUEAK.txt'],
    third_party_license_files=[],
    truffle_jars=[
        'graalsqueak:GRAALSQUEAK',
        'graalsqueak:GRAALSQUEAK_SHARED',
    ],
    support_distributions=[
        'graalsqueak:GRAALSQUEAK_GRAALVM_SUPPORT',
    ],
    launcher_configs=[
        mx_sdk.LanguageLauncherConfig(
            destination=SVM_TARGET,
            jar_distributions=[
                'graalsqueak:GRAALSQUEAK_LAUNCHER',
                'graalsqueak:GRAALSQUEAK_SHARED',
            ],
            main_class='%s.launcher.GraalSqueakLauncher' % PACKAGE_NAME,
            build_args=[
                # '--pgo-instrument',  # (uncomment to enable profiling)
                # '--pgo',  # (uncomment to recompile with profiling info)
            ],
            language=LANGUAGE_ID
        )
    ],
))


mx.update_commands(_suite, {
    'squeak': [_squeak, '[options]'],
    'squeak-svm': [_squeak_svm, ''],
})

mx_gate.add_gate_runner(_suite, _graalsqueak_gate_runner)

if mx.is_windows():
    # This patch works around "SSL: CERTIFICATE_VERIFY_FAILED" errors (See
    # https://www.python.org/dev/peps/pep-0476/).
    import ssl
    ssl._create_default_https_context = ssl._create_unverified_context
