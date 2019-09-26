#!/usr/bin/env bash
#
# Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
#
# Licensed under the MIT License.
#

set -e

readonly BASE_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")/../" && pwd)"
source "${BASE_DIRECTORY}/scripts/helpers.sh"

download_assert $@
