#!/usr/bin/env bash
set -e

readonly BASE_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")/../" && pwd)"
source "${BASE_DIRECTORY}/scripts/helpers.sh"

if [[ "${ARCH}" == "32bit" ]]; then
  ensure_test_image_32bit
  echo "Test images should be located at '${BASE_DIRECTORY}/images/test-32bit.image'."
else
  ensure_test_image_64bit
  echo "Test images should be located at '${BASE_DIRECTORY}/images/test-64bit.image'."
fi
