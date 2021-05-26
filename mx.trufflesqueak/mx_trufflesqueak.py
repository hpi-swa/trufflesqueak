#
# Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
#
# Licensed under the MIT License.
#

from __future__ import print_function

import argparse
import os
import sys

import mx
import mx_gate
import mx_sdk
import mx_truffle
import mx_unittest

_SUITE = mx.suite('trufflesqueak')

LANGUAGE_ID = 'smalltalk'
PACKAGE_NAME = 'de.hpi.swa.trufflesqueak'
BASE_DIR = os.path.dirname(os.path.dirname(os.path.realpath(__file__)))
BASE_VM_ARGS = [
    # Tweak Runtime
    '-Xss64M',  # Increase stack size (`-XX:ThreadStackSize=64M` not working)
]

if mx.is_darwin():
    BASE_VM_ARGS.append('-Xdock:name=TruffleSqueak')

BASE_VM_ARGS_TESTING = BASE_VM_ARGS[:]
BASE_VM_ARGS_TESTING.extend([
    # Tweak GC for GitHub Actions
    '-Xms4G',                   # Initial heap size
    '-XX:MetaspaceSize=32M',    # Initial size of Metaspaces
])

IS_JDK9_AND_LATER = mx.get_jdk(tag='default').javaCompliance > '1.8'

if IS_JDK9_AND_LATER:
    ADD_OPENS = [
        # Make Truffle.getRuntime() accessible for VM introspection
        '--add-opens=jdk.internal.vm.compiler/org.graalvm.compiler.truffle.runtime=ALL-UNNAMED',
        # Enable access to HostObject and others
        '--add-opens=org.graalvm.truffle/com.oracle.truffle.polyglot=ALL-UNNAMED',
        # Enable access to Truffle's SourceSection (for retrieving sources through interop)
        '--add-opens=org.graalvm.truffle/com.oracle.truffle.api.source=ALL-UNNAMED',
    ]
    BASE_VM_ARGS.extend(ADD_OPENS)
    BASE_VM_ARGS_TESTING.extend(ADD_OPENS)
else:
    # Tweaks for Java 8's Parallel GC (optimized for TruffleSqueak image)
    BASE_VM_ARGS.append('-XX:OldSize=256M')       # Initial tenured generation size
    BASE_VM_ARGS.append('-XX:NewSize=1G')         # Initial new generation size
    BASE_VM_ARGS.append('-XX:MetaspaceSize=32M')  # Initial size of Metaspaces

_COMPILER = mx.suite('compiler', fatalIfMissing=False)
_SVM = mx.suite('substratevm', fatalIfMissing=False)

if _COMPILER:
    # Tweak GraalVM Engine
    BASE_VM_ARGS.append('-Dpolyglot.engine.Mode=latency')
    BASE_VM_ARGS_TESTING.append('-Dpolyglot.engine.Mode=latency')

    BASE_VM_ARGS_TESTING.append('-Dpolyglot.engine.CompilationFailureAction=Diagnose')

def _graal_vm_args(args):
    graal_args = ['-Dpolyglot.engine.AllowExperimentalOptions=true']

    if args.trace_compilation:
        graal_args += ['-Dpolyglot.engine.TraceCompilation=true']

    if args.compilation_details:
        graal_args += ['-Dpolyglot.engine.TraceCompilationDetails=true']

    if args.compilation_polymorphism:
        graal_args += ['-Dpolyglot.engine.TraceCompilationPolymorphism=true']

    if args.compilation_stats:
        graal_args += ['-Dpolyglot.engine.CompilationStatistics=true']

    if args.expansion_histogram:
        graal_args += ['-Dpolyglot.engine.PrintExpansionHistogram=true']

    if args.instrument_boundaries:
        graal_args += [
            '-Dpolyglot.engine.InstrumentBoundaries=true',
            '-Dpolyglot.engine.InstrumentBoundariesPerInlineSite=true',
        ]

    if args.perf_warnings:
        graal_args += [
            '-Dpolyglot.engine.TreatPerformanceWarningsAsErrors=all',
            '-Dpolyglot.engine.TracePerformanceWarnings=all']

    if args.trace_invalidation:
        graal_args += [
            '-Dpolyglot.engine.TraceTransferToInterpreter=true',
            '-Dpolyglot.engine.TraceAssumptions=true',
        ]

    if args.trace_inlining:
        graal_args += ['-Dpolyglot.engine.TraceInlining=true']

    if args.trace_splitting:
        graal_args += [
            # '-Dpolyglot.engine.TraceSplitting=true',
            '-Dpolyglot.engine.TraceSplittingSummary=true',
        ]

    if args.igv:
        print('Sending Graal dumps to igv...')
        graal_args += [
            '-Dpolyglot.engine.BackgroundCompilation=false',
            # Use `Truffle:2` for graphs between each compiler phase
            '-Dgraal.Dump=Truffle:1',
            '-Dgraal.DumpOnError=true',
            '-Dgraal.PrintGraph=Network',
            # Disable some optimizations
            '-Dgraal.FullUnroll=false',
            '-Dgraal.PartialUnroll=false',
            '-Dgraal.LoopPeeling=false',
            '-Dgraal.LoopUnswitch=false',
            '-Dgraal.OptScheduleOutOfLoops=false',
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
        graal_args += ['-Dpolyglot.engine.BackgroundCompilation=false']

    if args.force_compilation:
        graal_args += ['-Dpolyglot.engine.CompileImmediately=true']

    if args.print_graal_options:
        graal_args += ['-XX:+JVMCIPrintProperties']

    graal_args += [
        '-Djvmci.Compiler=graal',
        '-XX:+UseJVMCICompiler',
        # '-XX:-UseJVMCINativeLibrary',  # to disable libgraal
    ]
    return graal_args


def _squeak(args, extra_vm_args=None, env=None, jdk=None, **kwargs):
    """run TruffleSqueak"""

    env = env if env else os.environ.copy()

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
    parser.add_argument('--disable-startup',
                        help='disable startup routine in headless mode',
                        dest='disable_startup',
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
        '-cd', '--compilation-details',
        help='print Truffle compilation details',
        dest='compilation_details', action='store_true', default=False)
    parser.add_argument(
        '-cp', '--compilation-polymorphism',
        help='print all polymorphic and generic nodes after each compilation',
        dest='compilation_polymorphism', action='store_true',
        default=False)
    parser.add_argument(
        '-cs', '--compilation-statistics',
        help='print Truffle compilation statistics at the end of a run',
        dest='compilation_stats', action='store_true', default=False)
    parser.add_argument(
        '-eh', '--expansion-histogram',
        help='print a histogram of all expanded Java methods',
        dest='expansion_histogram', action='store_true', default=False)
    parser.add_argument(
        '-ib', '--instrument-boundaries',
        help='instrument Truffle boundaries and output profiling information',
        dest='instrument_boundaries', action='store_true',
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

    vm_args = BASE_VM_ARGS + _get_runtime_jvm_args(jdk)

    if _COMPILER:
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

    vm_args.append('%s.launcher.TruffleSqueakLauncher' % PACKAGE_NAME)

    squeak_arguments = []
    if parsed_args.disable_interrupts:
        squeak_arguments.append(
            '--%s.disable-interrupts' % LANGUAGE_ID)
    if parsed_args.disable_startup:
        squeak_arguments.extend([
            '--experimental-options',
            '--%s.disable-startup' % LANGUAGE_ID])
    if parsed_args.headless:
        squeak_arguments.append('--%s.headless' % LANGUAGE_ID)
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
        parsed_args.log = 'interop=FINE'
    if parsed_args.trace_iterate_frames:
        parsed_args.log = 'iterate-frames=FINE'
    if parsed_args.trace_primitive_failures:
        parsed_args.log = 'primitives=FINE'
    if parsed_args.trace_process_switches:
        parsed_args.log = 'scheduling=FINE'
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
        jdk = mx.get_jdk(tag='jvmci' if _COMPILER else None)

    return mx.run_java(vm_args + squeak_arguments, jdk=jdk, **kwargs)


def _get_runtime_jvm_args(jdk):
    dists = ['TRUFFLESQUEAK', 'TRUFFLESQUEAK_LAUNCHER']

    is_graalvm = mx_truffle._is_graalvm(jdk or mx.get_jdk())

    if not is_graalvm:
        dists.append('TRUFFLE_NFI')
        if mx.suite('graal-js', fatalIfMissing=False):
            dists.append('GRAALJS')

    return mx.get_runtime_jvm_args(dists, jdk=jdk)


def _squeak_graalvm_launcher(args):
    """Build and run a GraalVM TruffleSqueak launcher"""

    dy = ['--dynamicimports', '/vm']
    mx.run_mx(dy + ['--env', 'ce-trufflesqueak', 'build'])
    out = mx.OutputCapture()
    mx.run_mx(dy + ["graalvm-home"], out=mx.TeeOutputCapture(out))
    launcher = os.path.join(out.data.strip(), "bin", "trufflesqueak").split("\n")[-1].strip()
    mx.log(launcher)
    if args:
        mx.run([launcher] + args)
    return launcher


def _trufflesqueak_gate_runner(args, tasks):
    os.environ['MX_GATE'] = 'true'
    supports_coverage = '--jacocout' in sys.argv

    _add_copyright_checks(tasks)
    # _add_tck_tests(tasks, supports_coverage)
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
    with mx_gate.Task('TruffleSqueak JUnit and SUnit tests',
                      tasks, tags=['test']) as t:
        if t:
            unittest_args = BASE_VM_ARGS_TESTING[:]
            if supports_coverage:
                unittest_args.extend(_get_jacoco_agent_args())
            unittest_args.extend(['--suite', 'trufflesqueak', '--very-verbose',
                                  '--color', '--enable-timing'])

            # Ensure Truffle TCK disabled (workaround needed since GraalVM 19.2.0)
            # import mx_truffle
            # mx_unittest._config_participants.remove(
            #     mx_truffle._unittest_config_participant_tck)
            mx_unittest.unittest(unittest_args)


# Extend `vmArgs` with `_get_runtime_jvm_args` when running `mx unittest`
def _unittest_config_participant(config):
    vmArgs, mainClass, mainClassArgs = config
    jdk = mx.get_jdk(tag='default')
    runtime_args = _get_runtime_jvm_args(jdk)
    # Remove the cp argument from the runtime args
    cp = None
    for i, cp in enumerate(runtime_args[:]):
        if cp == "-cp":
            cp = runtime_args[i + 1]
            runtime_args.remove("-cp")
            runtime_args.remove(cp)
            break
    # Attach remaining runtime args
    vmArgs += runtime_args
    # Merge the classpaths
    if cp:
        for i, arg in enumerate(vmArgs):
            if arg == "-cp":
                vmArgs[i + 1] += ":" + cp
    config = (vmArgs, mainClass, mainClassArgs)
    return config


mx_unittest.add_config_participant(_unittest_config_participant)


def _add_tck_tests(tasks, supports_coverage):
    with mx_gate.Task('TruffleSqueak TCK tests', tasks, tags=['test']) as t:
        if t:
            unittest_args = BASE_VM_ARGS_TESTING[:]
            if supports_coverage:
                unittest_args.extend(_get_jacoco_agent_args())
            test_image = _get_path_to_test_image()
            unittest_args.extend([
                '--color', '--fail-fast',
                '-Dtck.language=%s' % LANGUAGE_ID,
                '-Dpolyglot.%s.headless=true' % LANGUAGE_ID,
                '-Dpolyglot.%s.image-path=%s' % (LANGUAGE_ID, test_image),
                'com.oracle.truffle.tck.tests'])
            mx_unittest.unittest(unittest_args)


def _get_jacoco_agent_args():
    # Modified version of mx_gate.get_jacoco_agent_args()
    agentOptions = {
        'append': 'true',
        'includes': '%s.*' % PACKAGE_NAME,
        'destfile': mx_gate.get_jacoco_dest_file(),
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


def _enable_local_compression():
    def patched_init(self, *args, **kw_args):
        self._local_compress = kw_args.pop('localCompress', True) # Flip default to `True`
        self._remote_compress = kw_args.pop('remoteCompress', True)
        if self._local_compress and not self._remote_compress:
            mx.abort("Incompatible local/remote compression settings: local compression requires remote compression")
        super(mx.LayoutZIPDistribution, self).__init__(*args, compress=self._local_compress, **kw_args)

    mx.LayoutZIPDistribution.__init__ = patched_init


_enable_local_compression()


mx_sdk.register_graalvm_component(mx_sdk.GraalVmLanguage(
    suite=_SUITE,
    name='TruffleSqueak',
    short_name='st',
    dir_name=LANGUAGE_ID,
    license_files=[],  # already included in `TRUFFLESQUEAK_HOME`.
    third_party_license_files=[],
    dependencies=['Truffle'],
    truffle_jars=[
        'trufflesqueak:TRUFFLESQUEAK',
        'trufflesqueak:TRUFFLESQUEAK_SHARED',
    ],
    support_distributions=[
        'trufflesqueak:TRUFFLESQUEAK_HOME',
    ],
    launcher_configs=[
        mx_sdk.LanguageLauncherConfig(
            language=LANGUAGE_ID,
            destination='bin/<exe:trufflesqueak>',
            jar_distributions=['trufflesqueak:TRUFFLESQUEAK_LAUNCHER'],
            main_class='%s.launcher.TruffleSqueakLauncher' % PACKAGE_NAME,
            extra_jvm_args=BASE_VM_ARGS,
            build_args=[
                # '--pgo-instrument',  # (uncomment to enable profiling)
                # '--pgo',  # (uncomment to recompile with profiling info)
            ],
        )
    ],
    post_install_msg=(None if not _SVM else "\nNOTES:\n---------------\n" +
            "TruffleSqueak (SVM) requires SDL2 to be installed on your system:\n" +
            "- On Debian/Ubuntu, you can install SDL2 via `sudo apt-get install libsdl2-2.0`.\n" +
            "- On macOS, you can install SDL2 with Homebrew: `brew install sdl2`.\n\n" +
            "The pre-compiled native image is used by default and does not include other languages. " +
            "Run TruffleSqueak in JVM mode (via `trufflesqueak --jvm`) for polyglot access."),
))


mx.update_commands(_SUITE, {
    'squeak': [_squeak, '[options]'],
    'squeak-gvm': [_squeak_graalvm_launcher, '[options]'],
})

mx_gate.add_gate_runner(_SUITE, _trufflesqueak_gate_runner)

if mx.is_windows():
    # This patch works around "SSL: CERTIFICATE_VERIFY_FAILED" errors (See
    # https://www.python.org/dev/peps/pep-0476/).
    import ssl
    ssl._create_default_https_context = ssl._create_unverified_context
