#
# Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
# Copyright (c) 2021-2025 Oracle and/or its affiliates
#
# Licensed under the MIT License.
#

import os
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
    is_oracle_graalvm = "-community" not in os.getenv("JAVA_HOME")
    if is_oracle_graalvm and mx.get_os() == "linux":
        build_args.append("--gc=G1")
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
