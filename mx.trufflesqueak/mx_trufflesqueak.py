#
# Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
# Copyright (c) 2021-2026 Oracle and/or its affiliates
#
# Licensed under the MIT License.
#

import os
import glob
import zipfile
import json
import mx
import mx_gate
import mx_truffle
import mx_unittest

# re-export custom mx project classes so they can be used from suite.py
from mx_cmake import CMakeNinjaProject  # pylint: disable=unused-import
from mx_sdk_vm_ng import (  # pylint: disable=unused-import
    StandaloneLicenses,
    ThinLauncherProject,
    LanguageLibraryProject,
    DynamicPOMDistribution,
)

_SUITE = mx.suite("trufflesqueak")


# Called from suite.py
def trufflesqueak_standalone_deps():
    return mx_truffle.resolve_truffle_dist_names(use_optimized_runtime=True)


# Called from suite.py
def libsmalltalkvm_build_args():
    build_args = []
    selected_march = (
        "x86-64-v2"
        if mx.get_arch() == "amd64"
        else ("armv8.1-a" if mx.get_arch() == "aarch64" else "compatibility")
    )
    build_args.append(f"-march={selected_march}")

    java_home = os.getenv("JAVA_HOME", "")
    is_oracle_graalvm = "-community" not in java_home and java_home != ""
    if is_oracle_graalvm and mx.get_os() == "linux":
        build_args.append("--gc=G1")

    # 1. Setup the temporary auto-generated UI config folder (lives in mxbuild)
    auto_config_dir = os.path.join(_SUITE.dir, "mxbuild", "auto-jni-config")
    os.makedirs(auto_config_dir, exist_ok=True)

    # 2. Auto-generate the broad HumbleUI config
    cache_dir = os.path.expanduser("~/.mx/cache")
    target_jars = glob.glob(os.path.join(cache_dir, "**", "*.jar"), recursive=True)

    class_set = set()
    for jar in target_jars:
        try:
            with zipfile.ZipFile(jar, "r") as z:
                for file_path in z.namelist():
                    if (
                        file_path.endswith(".class")
                        and "io/github/humbleui" in file_path
                    ):
                        class_set.add(file_path.replace("/", ".")[:-6])
        except zipfile.BadZipFile:
            pass

    if class_set:
        jni_config = [
            {
                "name": cls,
                "allDeclaredConstructors": True,
                "allPublicConstructors": True,
                "allDeclaredMethods": True,
                "allPublicMethods": True,
                "allDeclaredFields": True,
                "allPublicFields": True,
            }
            for cls in sorted(class_set)
        ]

        with open(os.path.join(auto_config_dir, "jni-config.json"), "w") as f:
            json.dump(jni_config, f, indent=2)

        mx.log(f"Auto-generated JNI config for {len(class_set)} HumbleUI classes.")

        # 3. Feed the dynamic directory to GraalVM
        # (GraalVM automatically finds the META-INF.native-image/jwm-skija files)
        build_args.append(f"-H:ConfigurationFileDirectories={auto_config_dir}")

    return build_args


def _trufflesqueak_gate_runner(args, tasks):
    with mx_gate.Task("Check Copyrights", tasks, tags=[mx_gate.Tags.style]) as t:
        if t:
            if mx.checkcopyrights(["--primary"]) != 0:
                t.abort(
                    'Copyright errors found. Please run "mx '
                    'checkcopyrights --primary -- --fix" to fix them.'
                )
    with mx_gate.Task("TruffleSqueak JUnit and SUnit tests", tasks, tags=["test"]) as t:
        if t:
            mx_unittest.unittest(
                [
                    "--suite",
                    "trufflesqueak",
                    "--very-verbose",
                    "--color",
                    "--enable-timing",
                ]
            )


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


mx_gate.add_gate_runner(_SUITE, _trufflesqueak_gate_runner)
mx_unittest.add_config_participant(_unittest_config_participant)
