#!/usr/bin/env bash
set -e

readonly BASE_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")/../" && pwd)"
source "${BASE_DIRECTORY}/scripts/helpers.sh"

download_assert $@
