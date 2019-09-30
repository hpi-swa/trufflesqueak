#!/usr/bin/env bash
#
# Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
#
# Licensed under the MIT License.
#

readonly DEFAULT_GIT_TAG="0.9.0"
readonly IMAGE32_NAME="GraalSqueakTestImage-18699-32bit.zip"
readonly IMAGE64_NAME="GraalSqueakTestImage-18699-64bit.zip"
readonly GITHUB_SLUG="hpi-swa/graalsqueak"


if [[ -z "${BASE_DIRECTORY}" ]]; then
  echo '${BASE_DIRECTORY} is not set.' 1>&2
  echo "This file is intended to be included in other scripts!" 1>&2
  exit
fi

download_assert() {
  local filename=$1
  local git_tag="${2:-${DEFAULT_GIT_TAG}}"
  local target="${3:-$1}"
  curl -L -o "${target}" \
    "https://github.com/${GITHUB_SLUG}/releases/download/${git_tag}/${filename}"
}

ensure_test_image_32bit() {
  local target_dir="${BASE_DIRECTORY}/images"

  if [[ -f "${target_dir}/test-32bit.image" ]]; then
    return
  fi

  mkdir "${target_dir}" || true
  pushd "${target_dir}" > /dev/null

  download_assert "${IMAGE32_NAME}"
  unzip "${IMAGE32_NAME}"
  mv *.image test-32bit.image
  mv *.changes test-32bit.changes

  popd > /dev/null
}

ensure_test_image_64bit() {
  local target_dir="${BASE_DIRECTORY}/images"

  if [[ -f "${target_dir}/test-64bit.image" ]]; then
    return
  fi

  mkdir "${target_dir}" || true
  pushd "${target_dir}" > /dev/null

  download_assert "${IMAGE64_NAME}"
  unzip "${IMAGE64_NAME}"
  mv *.image test-64bit.image
  mv *.changes test-64bit.changes

  popd > /dev/null
}
