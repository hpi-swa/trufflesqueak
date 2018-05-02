#!/usr/bin/env bash
set -e

readonly BASE_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")/../" && pwd)"
source "${BASE_DIRECTORY}/scripts/helpers.sh"

ensure_test_image

MX_EXEC=$(locate_mx)
MX_PARAMETERS=$(get_mx_parameters "$@")

${MX_EXEC} ${MX_PARAMETERS} \
  --dynamicimports /compiler \
  --user-home "${BASE_DIRECTORY}" \
    gate $@
