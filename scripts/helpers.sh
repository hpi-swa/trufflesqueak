#!/usr/bin/env bash

readonly DEFAULT_GIT_TAG="0.8.4"
readonly IMAGE32_NAME="GraalSqueakTestImage-18618-32bit.zip"
readonly IMAGE64_NAME="GraalSqueakTestImage-18618-64bit.zip"
readonly GITHUB_SLUG="hpi-swa-lab/graalsqueak"
readonly MX_GIT="https://github.com/graalvm/mx.git"


if [[ -z "${BASE_DIRECTORY}" ]]; then
  echo '${BASE_DIRECTORY} is not set.' 1>&2
  echo "This file is intended to be included in other scripts!" 1>&2
  exit
fi
readonly MX_PATH="${BASE_DIRECTORY}/../mx"

locate_mx() {
  local mx_exec="mx"

  if [[ ! $(which "${mx_exec}" 2> /dev/null) ]]; then
    if [[ ! -d "${MX_PATH}" ]]; then
      read -p "mx not found. Would you like to clone it now? (y/N): " user_input
      if [[ "${user_input}" = "y" ]]; then
        pushd "${BASE_DIRECTORY}" > /dev/null
        git clone "${MX_GIT}" "${MX_PATH}"
        popd > /dev/null
      else
        exit 1
      fi
    fi
    mx_exec="${MX_PATH}/${mx_exec}"
  fi
  echo "${mx_exec}"
}

get_mx_parameters() {
  local mx_parameters=""

  if [[ "$@" == *"-v "* ]]; then  # enable mx's verbose mode, too.
    mx_parameters+="-v"
  fi
  echo "${mx_parameters}"
}

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
