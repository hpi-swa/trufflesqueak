# This file is intended to be included in other scripts

readonly IMAGE32_NAME="GraalSqueak-18163-32bit.zip"
readonly IMAGE32_ASSET_ID="8089597"
readonly IMAGE64_NAME="GraalSqueak-18163-64bit.zip"
readonly IMAGE64_ASSET_ID="8089598"
readonly GITHUB_SLUG="hpi-swa-lab/graalsqueak"
readonly MX_GIT="https://github.com/graalvm/mx.git"


[[ -z "${BASE_DIRECTORY}" ]] && echo "${BASE_DIRECTORY} is not set" && exit
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

ensure_test_image_32bit() {
  local target_dir="${BASE_DIRECTORY}/images"

  if [[ -f "${target_dir}/test-32bit.image" ]]; then
    return
  fi

  mkdir "${target_dir}" || true
  pushd "${target_dir}" > /dev/null

  curl -L -H 'Accept:application/octet-stream' -o "${IMAGE32_NAME}" \
    "https://${GITHUB_TOKEN}:@api.github.com/repos/${GITHUB_SLUG}/releases/assets/${IMAGE32_ASSET_ID}"
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

  curl -L -H 'Accept:application/octet-stream' -o "${IMAGE64_NAME}" \
    "https://${GITHUB_TOKEN}:@api.github.com/repos/${GITHUB_SLUG}/releases/assets/${IMAGE64_ASSET_ID}"
  unzip "${IMAGE64_NAME}"
  mv *.image test-64bit.image
  mv *.changes test-64bit.changes

  popd > /dev/null
}
