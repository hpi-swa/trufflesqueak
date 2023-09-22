#
# Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
# Copyright (c) 2021-2023 Oracle and/or its affiliates
#
# Licensed under the MIT License.
#

import os
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

LANGUAGE_ID = 'smalltalk'
PACKAGE_NAME = 'de.hpi.swa.trufflesqueak'
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


def _unittest_config_participant(config):
    (vmArgs, mainClass, mainClassArgs) = config
    vmArgs += ['-Dpolyglotimpl.DisableClassPathIsolation=true']
    vmArgs += ['--add-exports=java.base/jdk.internal.module=de.hpi.swa.trufflesqueak']
    mainClassArgs += ['-JUnitOpenPackages', 'de.hpi.swa.trufflesqueak/*=de.hpi.swa.trufflesqueak.test']
    mainClassArgs += ['-JUnitOpenPackages', 'de.hpi.swa.trufflesqueak/*=ALL-UNNAMED']
    return (vmArgs, mainClass, mainClassArgs)

mx_unittest.add_config_participant(_unittest_config_participant)

mx_truffle.should_add_tck_participant(False)

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


def _use_different_graalvm_home_for_native_image(extra_graalvm_home):

    def patched_native_image(self, build_args, output_file, allow_server=False, nonZeroIsFatal=True, out=None, err=None):
        build_args.remove('--macro:smalltalkvm-library')
        assert 'smalltalkvm' in output_file
        native_image_path = os.path.join(extra_graalvm_home, 'bin', mx.cmd_suffix('native-image'))
        dist_names = ['TRUFFLESQUEAK', 'TRUFFLESQUEAK_LAUNCHER', 'TRUFFLE_NFI_LIBFFI', 'TRUFFLE-ENTERPRISE', 'SDK-NATIVEBRIDGE'] + mx_truffle.resolve_truffle_dist_names(use_optimized_runtime=True, use_enterprise=True)
        selected_gc = 'G1' if mx.is_linux() else 'serial'
        build_command = [native_image_path] + build_args + mx.get_runtime_jvm_args(names=dist_names) + [
            '-o', os.path.splitext(output_file)[0],
            '--shared',
            '--gc=' + selected_gc,
            '--module', 'de.hpi.swa.trufflesqueak.launcher/%s.launcher.TruffleSqueakLauncher' % PACKAGE_NAME,
        ]
        mx.log('Running {} ...'.format(' '.join(build_command)))
        return mx.run(build_command)

    mx_sdk_vm_impl.SvmSupport.native_image = patched_native_image


extra_graalvm_home = os.getenv('EXTRA_GRAALVM_HOME')
if extra_graalvm_home:
    _use_different_graalvm_home_for_native_image(extra_graalvm_home)


mx_sdk_vm.register_vm_config('trufflesqueak-jar', ['sdk', 'sdkc', 'sdkni', 'ins', 'cov', 'dap', 'lsp', 'sdkl', 'pro', 'insight', 'insightheap', 'tfl', 'tfla', 'tflc', 'truffle-json', 'nfi', 'nfi-libffi', 'st'],
                                _SUITE, env_file='trufflesqueak-jar')
mx_sdk_vm.register_vm_config('trufflesqueak-jvm', ['ssmalltalkvm', 'sdk', 'sdkc', 'sdkni', 'ins', 'cov', 'dap', 'lsp', 'sdkl', 'pro', 'cmp', 'insight', 'insightheap', 'lg', 'svm', 'svmsl', 'tfl', 'tfla', 'tflc', 'truffle-json', 'nfi', 'nfi-libffi', 'svmt', 'st'],
                                _SUITE, env_file='trufflesqueak-jvm')
mx_sdk_vm.register_vm_config('trufflesqueak-native', ['sdk', 'sdkc', 'sdkni', 'ins', 'cov', 'dap', 'lsp', 'sdkl', 'pro', 'cmp', 'insight', 'insightheap', 'svm', 'svmsl', 'tfl', 'tfla', 'tflc', 'truffle-json', 'tflm', 'nfi', 'nfi-libffi', 'svmt', 'st'],
                                _SUITE, env_file='trufflesqueak-native')


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
    support_distributions=['trufflesqueak:TRUFFLESQUEAK_HOME'],
    library_configs=[
        mx_sdk.LanguageLibraryConfig(
            language=LANGUAGE_ID,
            jar_distributions=['trufflesqueak:TRUFFLESQUEAK_LAUNCHER'],
            main_class='%s.launcher.TruffleSqueakLauncher' % PACKAGE_NAME,
            build_args=[
                '-H:+DumpThreadStacksOnSignal',
                '-H:+DetectUserDirectoriesInImageHeap',
            ],
            destination='lib/<lib:%svm>' % LANGUAGE_ID,
            launchers=['bin/<exe:trufflesqueak>'],
            default_vm_args=[
                '--vm.Xms512M',
                '--vm.Xss16M',
                '--vm.-add-exports=java.base/jdk.internal.module=de.hpi.swa.trufflesqueak',
            ] + (['--vm.Xdock:name=TruffleSqueak'] if mx.is_darwin() else []),
        )
    ],
    stability='experimental',
    post_install_msg=None,
))


mx_gate.add_gate_runner(_SUITE, _trufflesqueak_gate_runner)

mx.update_commands('mx', {
    'spotbugs': [lambda x: 0, ''],  # spotbugs temporarily disabled
})
