#!/usr/bin/env bash

readonly DEFAULT_GIT_TAG="0.9.0"
readonly IMAGE32_NAME="GraalSqueakTestImage-18699-32bit.zip"
readonly IMAGE64_NAME="GraalSqueakTestImage-18699-64bit.zip"
readonly GITHUB_SLUG="hpi-swa-lab/graalsqueak"


if [[ -z "${BASE_DIRECTORY}" ]]; then
  echo '${BASE_DIRECTORY} is not set.' 1>&2
  echo "This file is intended to be included in other scripts!" 1>&2
  exit
fi

get_assert_id() {
  local filename=$1
  local git_tag=$2
  parser=". | map(select(.tag_name == \"${git_tag}\"))[0].assets | map(select(.name == \"${filename}\"))[0].id"
  curl "https://${GITHUB_TOKEN}:@api.github.com/repos/${GITHUB_SLUG}/releases" \
    | jq "$parser"
}

download_assert() {
  local filename=$1
  local git_tag="${2:-${DEFAULT_GIT_TAG}}"
  local target="${3:-$1}"
  local assert_id=$(get_assert_id "${filename}" "${git_tag}")
  curl -L -H 'Accept:application/octet-stream' -o "${target}" \
    "https://${GITHUB_TOKEN}:@api.github.com/repos/${GITHUB_SLUG}/releases/assets/${assert_id}"
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
