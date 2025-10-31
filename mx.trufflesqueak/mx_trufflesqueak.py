#
# Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
# Copyright (c) 2021-2025 Oracle and/or its affiliates
#
# Licensed under the MIT License.
#

import os
import mx
import mx_truffle

# re-export custom mx project classes so they can be used from suite.py
from mx_cmake import CMakeNinjaProject  # pylint: disable=unused-import
from mx_sdk_vm_ng import (  # pylint: disable=unused-import
    StandaloneLicenses,
    ThinLauncherProject,
    LanguageLibraryProject,
    DynamicPOMDistribution,
)


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
