#
# Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
# Copyright (c) 2021-2025 Oracle and/or its affiliates
#
# Licensed under the MIT License.
#

import os
import sys

import mx
import mx_gate
import mx_unittest

import mx_sdk
import mx_sdk_vm
import mx_sdk_vm_impl
import mx_truffle

# re-export custom mx project classes so they can be used from suite.py
from mx_cmake import CMakeNinjaProject  # pylint: disable=unused-import

_SUITE = mx.suite("trufflesqueak")
_COMPILER = mx.suite("compiler", fatalIfMissing=False)

LANGUAGE_ID = "smalltalk"
PACKAGE_NAME = "de.hpi.swa.trufflesqueak"
BASE_DIR = os.path.dirname(os.path.dirname(os.path.realpath(__file__)))
VM_ARGS_TESTING = [
    # Make ReflectionUtils work
    "--add-exports=java.base/jdk.internal.module=ALL-UNNAMED",
    # Tweak GC for GitHub Actions
    "-Xms6G",  # Initial heap size
    "-XX:MetaspaceSize=32M",  # Initial size of Metaspaces
]

if _COMPILER:
    # Tweak GraalVM Engine
    VM_ARGS_TESTING.append("-Dpolyglot.engine.Mode=latency")
    VM_ARGS_TESTING.append("-Dpolyglot.engine.CompilationFailureAction=Diagnose")


def _trufflesqueak_gate_runner(args, tasks):
    os.environ["MX_GATE"] = "true"
    supports_coverage = "--jacocout" in sys.argv

    _add_copyright_checks(tasks)
    # _add_tck_tests(tasks, supports_coverage)
    _add_unit_tests(tasks, supports_coverage)

    if supports_coverage:
        with mx_gate.Task("Report Code Coverage", tasks, tags=["test"]) as t:
            if t:
                mx.command_function("jacocoreport")(["--format", "xml", "."])


def _add_copyright_checks(tasks):
    with mx_gate.Task("Check Copyrights", tasks, tags=[mx_gate.Tags.style]) as t:
        if t:
            if mx.checkcopyrights(["--primary"]) != 0:
                t.abort(
                    'Copyright errors found. Please run "mx '
                    'checkcopyrights --primary -- --fix" to fix them.'
                )


def _add_unit_tests(tasks, supports_coverage):
    with mx_gate.Task("TruffleSqueak JUnit and SUnit tests", tasks, tags=["test"]) as t:
        if t:
            unittest_args = VM_ARGS_TESTING[:]
            if supports_coverage:
                unittest_args.extend(_get_jacoco_agent_args())
            unittest_args.extend(
                [
                    "--suite",
                    "trufflesqueak",
                    "--very-verbose",
                    "--color",
                    "--enable-timing",
                ]
            )

            # Ensure Truffle TCK disabled (workaround needed since GraalVM 19.2.0)
            # import mx_truffle
            # mx_unittest._config_participants.remove(
            #     mx_truffle._unittest_config_participant_tck)
            mx_unittest.unittest(unittest_args)


def _unittest_config_participant(config):
    (vmArgs, mainClass, mainClassArgs) = config
    vmArgs += ["-Dpolyglotimpl.DisableClassPathIsolation=true"]
    vmArgs += ["--add-exports=java.base/jdk.internal.module=de.hpi.swa.trufflesqueak"]
    mainClassArgs += [
        "-JUnitOpenPackages",
        "de.hpi.swa.trufflesqueak/*=de.hpi.swa.trufflesqueak.test",
    ]
    mainClassArgs += ["-JUnitOpenPackages", "de.hpi.swa.trufflesqueak/*=ALL-UNNAMED"]
    return (vmArgs, mainClass, mainClassArgs)


mx_unittest.add_config_participant(_unittest_config_participant)


def _add_tck_tests(tasks, supports_coverage):
    with mx_gate.Task("TruffleSqueak TCK tests", tasks, tags=["test"]) as t:
        if t:
            unittest_args = VM_ARGS_TESTING[:]
            if supports_coverage:
                unittest_args.extend(_get_jacoco_agent_args())
            test_image = _get_path_to_test_image()
            unittest_args.extend(
                [
                    "--color",
                    "--fail-fast",
                    "-Dtck.language=%s" % LANGUAGE_ID,
                    "-Dpolyglot.%s.headless=true" % LANGUAGE_ID,
                    "-Dpolyglot.%s.image-path=%s" % (LANGUAGE_ID, test_image),
                    "com.oracle.truffle.tck.tests",
                ]
            )
            mx_unittest.unittest(unittest_args)


def _get_jacoco_agent_args():
    # Modified version of mx_gate.get_jacoco_agent_args()
    agentOptions = {
        "append": "true",
        "includes": "%s.*" % PACKAGE_NAME,
        "destfile": mx_gate.get_jacoco_dest_file(),
    }
    return [
        "-javaagent:"
        + mx_gate.get_jacoco_agent_path(True)
        + "="
        + ",".join([k + "=" + v for k, v in agentOptions.items()])
    ]


def _get_path_to_test_image():
    images_dir = os.path.join(BASE_DIR, "images")
    image_64bit = os.path.join(images_dir, "test-64bit.image")
    if os.path.isfile(image_64bit):
        return image_64bit
    mx.abort("Unable to locate test image.")


def _enable_local_compression():
    def patched_init(self, *args, **kw_args):
        self._local_compress = kw_args.pop(
            "localCompress", True
        )  # Flip default to `True`
        self._remote_compress = kw_args.pop("remoteCompress", True)
        if self._local_compress and not self._remote_compress:
            mx.abort(
                "Incompatible local/remote compression settings: local compression requires remote compression"
            )
        super(mx.LayoutZIPDistribution, self).__init__(
            *args, compress=self._local_compress, **kw_args
        )

    mx.LayoutZIPDistribution.__init__ = patched_init


_enable_local_compression()


def _patch_svm_support_native_image():
    native_image_original = mx_sdk_vm_impl.SvmSupport.native_image

    def patched_native_image(self, build_args, output_file, out=None, err=None):
        if "smalltalkvm" not in output_file:
            return native_image_original(self, build_args, output_file, out, err)
        is_oracle_graalvm = False
        extra_graalvm_home = os.getenv("EXTRA_GRAALVM_HOME")
        if extra_graalvm_home:
            native_image_bin = os.path.join(
                extra_graalvm_home, "bin", mx.cmd_suffix("native-image")
            )
            is_oracle_graalvm = "-community" not in extra_graalvm_home
        else:
            stage1 = mx_sdk_vm_impl.get_stage1_graalvm_distribution()
            native_image_project_name = (
                mx_sdk_vm_impl.GraalVmLauncher.launcher_project_name(
                    mx_sdk.LauncherConfig(mx.exe_suffix("native-image"), [], "", []),
                    stage1=True,
                )
            )
            native_image_bin = os.path.join(
                stage1.output,
                stage1.find_single_source_location(
                    "dependency:" + native_image_project_name
                ),
            )
        build_args.remove("--macro:smalltalkvm-library")
        dist_names = (
            [
                "TRUFFLESQUEAK",
                "TRUFFLESQUEAK_LAUNCHER",
                "TRUFFLE_NFI_LIBFFI",
                "SDK-NATIVEBRIDGE",
            ]
            + (["TRUFFLE-ENTERPRISE"] if is_oracle_graalvm else [])
            + mx_truffle.resolve_truffle_dist_names(
                use_optimized_runtime=True, use_enterprise=True
            )
        )
        selected_march = (
            "x86-64-v2"
            if mx.get_arch() == "amd64"
            else ("armv8.1-a" if mx.get_arch() == "aarch64" else "compatibility")
        )
        selected_gc = "G1" if is_oracle_graalvm and mx.is_linux() else "serial"
        build_command = (
            [native_image_bin]
            + build_args
            + mx.get_runtime_jvm_args(names=dist_names)
            + [
                "-o",
                os.path.splitext(output_file)[0],
                "--shared",
                "-march=" + selected_march,
                "--gc=" + selected_gc,
                "--module",
                "de.hpi.swa.trufflesqueak.launcher/%s.launcher.TruffleSqueakLauncher"
                % PACKAGE_NAME,
            ]
        )
        mx.log("Running {} ...".format(" ".join(build_command)))
        return mx.run(build_command)

    mx_sdk_vm_impl.SvmSupport.native_image = patched_native_image


_patch_svm_support_native_image()


mx_sdk_vm.register_vm_config(
    "trufflesqueak-jar",
    [
        "sdk",
        "sdkc",
        "sdkni",
        "ins",
        "cov",
        "dap",
        "lsp",
        "sdkl",
        "pro",
        "insight",
        "insightheap",
        "tfl",
        "tfla",
        "tflc",
        "truffle-json",
        "nfi",
        "nfi-libffi",
        "st",
    ],
    _SUITE,
    env_file="trufflesqueak-jar",
)
mx_sdk_vm.register_vm_config(
    "trufflesqueak-jvm",
    [
        "ssmalltalkvm",
        "sdk",
        "sdkc",
        "sdkni",
        "ins",
        "cov",
        "dap",
        "lsp",
        "sdkl",
        "pro",
        "cmp",
        "insight",
        "insightheap",
        "lg",
        "svm",
        "svmsl",
        "tfl",
        "tfla",
        "tflc",
        "truffle-json",
        "nfi",
        "nfi-libffi",
        "svmt",
        "st",
    ],
    _SUITE,
    env_file="trufflesqueak-jvm",
)
mx_sdk_vm.register_vm_config(
    "trufflesqueak-native",
    [
        "sdk",
        "sdkc",
        "sdkni",
        "ins",
        "cov",
        "dap",
        "lsp",
        "sdkl",
        "pro",
        "cmp",
        "insight",
        "insightheap",
        "svm",
        "svmsl",
        "tfl",
        "tfla",
        "tflc",
        "truffle-json",
        "tflm",
        "nfi",
        "nfi-libffi",
        "svmt",
        "st",
    ],
    _SUITE,
    env_file="trufflesqueak-native",
)


mx_sdk.register_graalvm_component(
    mx_sdk.GraalVmLanguage(
        suite=_SUITE,
        name="TruffleSqueak",
        short_name="st",
        dir_name=LANGUAGE_ID,
        standalone_dir_name="trufflesqueak-<version>-<graalvm_os>-<arch>",
        license_files=[],  # already included in `TRUFFLESQUEAK_HOME`.
        third_party_license_files=[],
        dependencies=["Truffle", "Truffle NFI", "Truffle NFI LIBFFI"],
        standalone_dependencies={},
        truffle_jars=[
            "trufflesqueak:TRUFFLESQUEAK",
            "trufflesqueak:TRUFFLESQUEAK_SHARED",
            "trufflesqueak:BOUNCYCASTLE-PROVIDER",
            "trufflesqueak:BOUNCYCASTLE-PKIX",
            "trufflesqueak:BOUNCYCASTLE-UTIL",
        ],
        support_distributions=["trufflesqueak:TRUFFLESQUEAK_HOME"],
        library_configs=[
            mx_sdk.LanguageLibraryConfig(
                language=LANGUAGE_ID,
                launchers=[
                    "bin/<exe:trufflesqueak>",
                    "bin/<exe:trufflesqueak-polyglot-get>",
                ],
                jar_distributions=[
                    "trufflesqueak:TRUFFLESQUEAK_LAUNCHER",
                    "sdk:MAVEN_DOWNLOADER",
                ],
                main_class="%s.launcher.TruffleSqueakLauncher" % PACKAGE_NAME,
                build_args=[
                    "-H:+DumpThreadStacksOnSignal",
                    "-H:+DetectUserDirectoriesInImageHeap",
                ],
                default_vm_args=[
                    "--vm.Xms512M",
                    "--vm.-add-exports=java.base/jdk.internal.module=de.hpi.swa.trufflesqueak",
                ],
            )
        ],
        stability="experimental",
        post_install_msg=None,
    )
)


mx_gate.add_gate_runner(_SUITE, _trufflesqueak_gate_runner)
