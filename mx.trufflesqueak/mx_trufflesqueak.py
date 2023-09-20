#
# Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
# Copyright (c) 2021-2023 Oracle and/or its affiliates
#
# Licensed under the MIT License.
#

import os
import shutil
import sys

import mx
import mx_gate
import mx_sdk
import mx_sdk_vm
import mx_sdk_vm_impl
import mx_truffle
import mx_unittest

_SUITE = mx.suite('trufflesqueak')
_COMPILER = mx.suite('compiler', fatalIfMissing=False)
_SVM = mx.suite('substratevm', fatalIfMissing=False)

LANGUAGE_ID = 'smalltalk'
PACKAGE_NAME = 'de.hpi.swa.trufflesqueak'
USE_LIBRARY_LAUNCHER = True
BASE_DIR = os.path.dirname(os.path.dirname(os.path.realpath(__file__)))
VM_ARGS_TESTING = [
    # Tweak Runtime
    '-Xss16M',  # Increase stack size

    # Make ReflectionUtils work
    '--add-exports=java.base/jdk.internal.module=ALL-UNNAMED',

    # Tweak GC for GitHub Actions
    '-Xms6G',                   # Initial heap size
    '-XX:MetaspaceSize=32M',    # Initial size of Metaspaces
]

if _COMPILER:
    # Tweak GraalVM Engine
    VM_ARGS_TESTING.append('-Dpolyglot.engine.Mode=latency')
    VM_ARGS_TESTING.append('-Dpolyglot.engine.CompilationFailureAction=Diagnose')


def _get_runtime_jvm_args(jdk):
    dists = ['TRUFFLESQUEAK', 'TRUFFLESQUEAK_LAUNCHER']

    is_graalvm = mx_truffle._is_graalvm(jdk or mx.get_jdk())

    if not is_graalvm:
        dists.append('TRUFFLE_NFI')
        if mx.suite('graal-js', fatalIfMissing=False):
            dists.append('GRAALJS')

    return mx.get_runtime_jvm_args(dists, jdk=jdk)


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
            unittest_args = VM_ARGS_TESTING[:]
            if supports_coverage:
                unittest_args.extend(_get_jacoco_agent_args())
            unittest_args.extend(['--suite', 'trufflesqueak', '--very-verbose',
                                  '--color', '--enable-timing'])
            if _COMPILER:
                unittest_args.extend(['-Dgraal.CompilationFailureAction=ExitVM',
                                      '-Dpolyglot.compiler.TreatPerformanceWarningsAsErrors=call,instanceof,store,trivial',
                                      '-Dpolyglot.engine.CompilationFailureAction=ExitVM',
                                      '-Dpolyglot.engine.CompilationStatistics=true'])

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
        if cp == '-cp':
            cp = runtime_args[i + 1]
            runtime_args.remove('-cp')
            runtime_args.remove(cp)
            break
    # Attach remaining runtime args
    vmArgs += runtime_args
    # Merge the classpaths
    if cp:
        for i, arg in enumerate(vmArgs):
            if arg == '-cp':
                vmArgs[i + 1] += ':' + cp
    config = (vmArgs, mainClass, mainClassArgs)
    return config


mx_unittest.add_config_participant(_unittest_config_participant)


def _add_tck_tests(tasks, supports_coverage):
    with mx_gate.Task('TruffleSqueak TCK tests', tasks, tags=['test']) as t:
        if t:
            unittest_args = VM_ARGS_TESTING[:]
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
    mx.abort('Unable to locate test image.')


def _enable_local_compression():
    def patched_init(self, *args, **kw_args):
        self._local_compress = kw_args.pop('localCompress', True) # Flip default to `True`
        self._remote_compress = kw_args.pop('remoteCompress', True)
        if self._local_compress and not self._remote_compress:
            mx.abort('Incompatible local/remote compression settings: local compression requires remote compression')
        super(mx.LayoutZIPDistribution, self).__init__(*args, compress=self._local_compress, **kw_args)

    mx.LayoutZIPDistribution.__init__ = patched_init


_enable_local_compression()


def _copy_macro_and_language_jars():
    staged_dist = mx_sdk_vm_impl.get_stage1_graalvm_distribution()
    staged_graalvm_home = os.path.join(staged_dist.output, staged_dist.jdk_base)
    # Copy macros
    for macro_dir_name in ['smalltalkvm-library', 'truffle']:
        macro_src_dir = os.path.join(staged_graalvm_home, 'lib', 'svm', 'macros', macro_dir_name)
        macro_target_dir = os.path.join(extra_graalvm_home, 'lib', 'svm', 'macros', macro_dir_name)
        if not os.path.exists(macro_src_dir):
            mx.abort(f'Unable to locate macro at "{macro_src_dir}".')
        if not os.path.exists(macro_target_dir):
            shutil.copytree(macro_src_dir, macro_target_dir)
        else:
            mx.warn(f'Macros already copied to "{macro_target_dir}".')
    # Copy language directories
    for language_dir_name in ['smalltalk', 'nfi']:
        language_src_dir = os.path.join(staged_graalvm_home, 'languages', language_dir_name)
        language_target_dir = os.path.join(extra_graalvm_home, 'languages', language_dir_name)
        if not os.path.exists(language_src_dir):
            mx.abort(f'Unable to locate language JARs at "{language_src_dir}".')
        if not os.path.exists(language_target_dir):
            shutil.copytree(language_src_dir, language_target_dir)
        else:
            mx.warn(f'Language JARs already copied to "{language_target_dir}".')
    # Copy Truffle JARs
    for truffle_jar_name in ['jniutils.jar', 'locator.jar', 'truffle-api.jar', 'truffle-runtime.jar']:
        truffle_src_dir = os.path.join(staged_graalvm_home, 'lib', 'truffle', truffle_jar_name)
        truffle_target_dir = os.path.join(extra_graalvm_home, 'lib', 'truffle', truffle_jar_name)
        if not os.path.exists(truffle_src_dir):
            mx.abort(f'Unable to locate Truffle JAR at "{truffle_src_dir}".')
        if not os.path.exists(truffle_target_dir):
            shutil.copyfile(truffle_src_dir, truffle_target_dir)
        else:
            mx.warn(f'Truffle JAR already copied to "{truffle_target_dir}".')
    # Copy additional JARs
    for jar_name in ['trufflesqueak-launcher.jar', 'launcher-common.jar', 'jline3.jar']:
        launcher_src_dir = os.path.join(staged_graalvm_home, 'lib', 'graalvm', jar_name)
        launcher_target_dir = os.path.join(extra_graalvm_home, 'lib', 'graalvm', jar_name)
        if not os.path.exists(launcher_src_dir):
            mx.abort(f'Unable to locate JAR at "{launcher_src_dir}".')
        if not os.path.exists(launcher_target_dir):
            shutil.copyfile(launcher_src_dir, launcher_target_dir)
        else:
            mx.warn(f'JAR already copied to "{launcher_target_dir}".')


def _use_different_graalvm_home_for_native_image(extra_graalvm_home):

    def patched_native_image(self, build_args, output_file, allow_server=False, nonZeroIsFatal=True, out=None, err=None):
        mx.log(f'Using EXTRA_GRAALVM_HOME at "{extra_graalvm_home}" for building...')
        assert self._svm_supported
        _copy_macro_and_language_jars()
        native_image_bin = os.path.join(extra_graalvm_home, 'bin', mx.cmd_suffix('native-image'))
        native_image_command = [native_image_bin] + build_args
        output_directory = os.path.dirname(output_file)
        selected_gc = 'G1' if mx.is_linux() and mx.get_arch() == 'amd64' else 'serial'
        native_image_command += [
            '-H:Path=' + output_directory or ".",
            '--gc=' + selected_gc,
        ]
        return mx.run(native_image_command, nonZeroIsFatal=nonZeroIsFatal, out=out, err=err)

    mx_sdk_vm_impl.SvmSupport.native_image = patched_native_image


extra_graalvm_home = os.getenv('EXTRA_GRAALVM_HOME')
if extra_graalvm_home:
    _use_different_graalvm_home_for_native_image(extra_graalvm_home)


mx_sdk_vm.register_vm_config('trufflesqueak-jar', ['cov', 'dap', 'ins', 'insight', 'insightheap', 'lsp', 'nfi', 'nfi-libffi', 'pro', 'sdk', 'sdkc', 'sdkni', 'sdkl', 'st', 'tfl', 'tfla', 'tflc', 'truffle-json'],
                                _SUITE, env_file='trufflesqueak-jar')
mx_sdk_vm.register_vm_config('trufflesqueak-standalone', ['cmp', 'cov', 'dap', 'ins', 'insight', 'insightheap', 'lsp', 'nfi', 'nfi-libffi', 'pro', 'sdk', 'sdkc', 'sdkl', 'sdkni', 'st', 'svm', 'svmsl', 'svmt', 'tfl', 'tfla', 'tflc', 'tflm', 'truffle-json'],
                                _SUITE, env_file='trufflesqueak-standalone')


LAUNCHER_DESTINATION = 'bin/<exe:trufflesqueak>' if _SVM else 'bin/<exe:trufflesqueak-launcher>'

BASE_LANGUAGE_CONFIG = {
    'language': LANGUAGE_ID,
    'jar_distributions': ['trufflesqueak:TRUFFLESQUEAK_LAUNCHER'],
    'main_class': '%s.launcher.TruffleSqueakLauncher' % PACKAGE_NAME,
    'build_args': [
        '-H:+ReportExceptionStackTraces',
        '-H:+DumpThreadStacksOnSignal',
        '-H:+DetectUserDirectoriesInImageHeap',
        '-H:+TruffleCheckBlockListMethods',
    ],
}

mx_sdk.register_graalvm_component(mx_sdk.GraalVmLanguage(
    suite=_SUITE,
    name='TruffleSqueak',
    short_name='st',
    dir_name=LANGUAGE_ID,
    standalone_dir_name='trufflesqueak-<version>-<graalvm_os>-<arch>',
    license_files=[],  # already included in `TRUFFLESQUEAK_HOME`.
    third_party_license_files=[],
    dependencies=['Truffle', 'Truffle NFI', 'Truffle NFI LIBFFI'],
    standalone_dependencies={},
    truffle_jars=[
        'trufflesqueak:TRUFFLESQUEAK',
        'trufflesqueak:TRUFFLESQUEAK_SHARED',
        'trufflesqueak:BOUNCYCASTLE-PROVIDER',
        'trufflesqueak:BOUNCYCASTLE-PKIX',
        'trufflesqueak:BOUNCYCASTLE-UTIL',
    ],
    support_distributions=['trufflesqueak:TRUFFLESQUEAK_HOME'] + [] if _SVM else ['trufflesqueak:TRUFFLESQUEAK_LAUNCHER_SCRIPTS'],
    provided_executables=[] if _SVM else ['bin/<cmd:trufflesqueak>'],
    launcher_configs=[
        mx_sdk.LanguageLauncherConfig(destination=LAUNCHER_DESTINATION, **BASE_LANGUAGE_CONFIG)
    ] if not USE_LIBRARY_LAUNCHER else [],
    library_configs=[
        mx_sdk.LanguageLibraryConfig(destination='lib/<lib:%svm>' % LANGUAGE_ID, launchers=[LAUNCHER_DESTINATION], **BASE_LANGUAGE_CONFIG)
    ] if USE_LIBRARY_LAUNCHER else [],
    stability='experimental',
    post_install_msg=None,
))


mx_gate.add_gate_runner(_SUITE, _trufflesqueak_gate_runner)

mx.update_commands('mx', {
    'spotbugs': [lambda x: 0, ''],  # spotbugs temporarily disabled
})
