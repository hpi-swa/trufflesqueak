# This file is intended to be included in other scripts

readonly IMAGE_NAME="TestImageWithVMMaker.zip"
readonly IMAGE_URL="https://dl.bintray.com/hpi-swa-lab/GraalSqueak/images/${IMAGE_NAME}"
readonly MX_GIT="https://github.com/graalvm/mx.git"


[[ -z "${BASE_DIRECTORY}" ]] && echo "${BASE_DIRECTORY} is not set" && exit
readonly MX_PATH_SELF_CONTAINED="${BASE_DIRECTORY}/mx/mx"

locate_mx() {
  local mx_exec="mx"

  if [[ ! $(which "${mx_exec}" 2> /dev/null) ]]; then
    if [[ ! -f "${MX_PATH_SELF_CONTAINED}" ]]; then
      read -p "mx not found. Would you like to clone it now? (y/N): " user_input
      if [[ "${user_input}" = "y" ]]; then
        pushd "${BASE_DIRECTORY}" > /dev/null
        git clone "${MX_GIT}"
        popd > /dev/null
      else
        exit 1
      fi
    fi
    mx_exec="${MX_PATH_SELF_CONTAINED}"
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

ensure_test_image() {
  local target_dir="${BASE_DIRECTORY}/images"

  if [[ -f "${target_dir}/test.image" ]]; then
    return
  fi

  mkdir "${target_dir}"
  pushd "${target_dir}" > /dev/null
  curl -sLO "${IMAGE_URL}"
  unzip "${IMAGE_NAME}"
  mv *.image test.image
  mv *.changes test.changes
  popd > /dev/null
}
