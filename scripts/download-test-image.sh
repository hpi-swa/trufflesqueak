#!/usr/bin/env bash
set -e

readonly BASE_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")/../" && pwd)"
source "${BASE_DIRECTORY}/scripts/helpers.sh"

ensure_test_image

echo "Test image should be located at '${BASE_DIRECTORY}/images/test.image'."
