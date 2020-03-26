#!/usr/bin/env bash
#
# Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
#
# Licensed under the MIT License.
#

set -o errexit
set -o errtrace
set -o pipefail
set -o nounset

readonly SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")/" && pwd)"
readonly BASE_DIRECTORY="$(dirname "${SCRIPT_DIRECTORY}")"

# Load metadata from suite.py
readonly py_export=$(cat <<-END
from suite import suite;
vars= ' '.join(['DEP_%s=%s' % (k.upper(), v)
  for k, v in suite['graalsqueak:dependencyMap'].items()]);
slug = '/'.join(suite['url'].split('/')[-2:]);
print('export %s GITHUB_SLUG=%s' % (vars, slug))
END
)
$(cd "${SCRIPT_DIRECTORY}" && python -c "${py_export}")
([[ -z "${DEP_GRAALVM}" ]] || [[ -z "${GITHUB_SLUG}" ]]) && \
  echo "Failed to load values from dependencyMap and GitHub slug." 1>&2 && exit 1

OS_NAME=$(uname -s | tr '[:upper:]' '[:lower:]')
[[ "${OS_NAME}" == msys* || "${OS_NAME}" == cygwin* || "${OS_NAME}" == mingw* ]] && export OS_NAME="windows"
JAVA_HOME_SUFFIX="" && [[ "${OS_NAME}" == "darwin" ]] && JAVA_HOME_SUFFIX="/Contents/Home"
readonly OS_NAME JAVA_HOME_SUFFIX


add-path() {
  echo "::add-path::$(resolve-path $1)"
}

deploy-asset() {
  if ! [[ "$1" =~ ^refs\/tags\/[[:digit:]] ]]; then
    echo "Skipping deployment step (ref does not start with a digit)"
    exit 0
  fi
  local git_ref=${1:10} # cut off 'refs/tags/'
  local filename=$2
  local auth="Authorization: token $3"
  local release_id

  tag_result=$(curl -L --retry 3 --retry-connrefused --retry-delay 2 -sH "${auth}" \
    "https://api.github.com/repos/${GITHUB_SLUG}/releases/tags/${git_ref}")
  
  if echo "${tag_result}" | grep -q '"id":'; then
    release_id=$(echo "${tag_result}" | grep '"id":' | head -n 1 | sed 's/[^0-9]*//g')
    echo "Found GitHub release #${release_id} for ${git_ref}"
  else
    # Retry (in case release was just created by some other worker)
    tag_result=$(curl -L --retry 3 --retry-connrefused --retry-delay 2 -sH "${auth}" \
    "https://api.github.com/repos/${GITHUB_SLUG}/releases/tags/${git_ref}")
  
    if echo "${tag_result}" | grep -q '"id":'; then
      release_id=$(echo "${tag_result}" | grep '"id":' | head -n 1 | sed 's/[^0-9]*//g')
      echo "Found GitHub release #${release_id} for ${git_ref}"
    else
      create_result=$(curl -sH "${auth}" \
        --data "{\"tag_name\": \"${git_ref}\",
                \"name\": \"${git_ref}\",
                \"body\": \"\",
                \"draft\": false,
                \"prerelease\": false}" \
        "https://api.github.com/repos/${GITHUB_SLUG}/releases")
      if echo "${create_result}" | grep -q '"id":'; then
        release_id=$(echo "${create_result}" | grep '"id":' | head -n 1 | sed 's/[^0-9]*//g')
        echo "Created GitHub release #${release_id} for ${git_ref}"
      else
        echo "Failed to create GitHub release for ${git_ref}"
        exit 1
      fi
    fi
  fi

  curl --fail -o /dev/null -w "%{http_code}" \
    -H "${auth}" -H "Content-Type: application/zip" \
    --data-binary @"${filename}" \
    "https://uploads.github.com/repos/${GITHUB_SLUG}/releases/${release_id}/assets?name=${filename}"
}

download-asset() {
  local filename=$1
  local git_tag=$2
  local target="${3:-$1}"

  curl -s -L --retry 3 --retry-connrefused --retry-delay 2 -o "${target}" \
    "https://github.com/${GITHUB_SLUG}/releases/download/${git_tag}/${filename}"
}

download-graalsqueak-image() {
  local target_dir="${BASE_DIRECTORY}/src/resources"

  pushd "${target_dir}" > /dev/null

  download-asset "${DEP_IMAGE}" "${DEP_IMAGE_TAG}"
  unzip -qq "${DEP_IMAGE}"
  rm -f "${DEP_IMAGE}"

  popd > /dev/null

  echo "[Test image downloaded successfully]"
}

enable-jdk() {
  add-path "$1/bin"
  set-env "JAVA_HOME" "$(resolve-path $1)"
}

ensure-test-image() {
  local target_dir="${BASE_DIRECTORY}/images"

  if [[ -f "${target_dir}/test-64bit.image" ]]; then
    return
  fi

  mkdir "${target_dir}" || true
  pushd "${target_dir}" > /dev/null

  download-asset "${DEP_TEST_IMAGE}" "${DEP_TEST_IMAGE_TAG}"
  unzip -qq "${DEP_TEST_IMAGE}"
  mv ./*.image test-64bit.image
  mv ./*.changes test-64bit.changes

  popd > /dev/null

  echo "[Test image downloaded successfully]"
}

installable-filename() {
  local java_version=$1
  local git_describe=$(git describe --tags --always)
  local git_short_commit=$(git log -1 --format="%h")
  local git_description="${git_describe:-${git_short_commit}}"
  echo "graalsqueak-installable-${java_version}-${OS_NAME}-amd64-${git_description}-for-GraalVM-${DEP_GRAALVM}.jar"
}

resolve-path() {
  if [[ "${OS_NAME}" == "windows" ]]; then
    # Convert Unix path to Windows path
    echo $1 | sed 's/\/c/C:/g' | sed 's/\//\\/g'
  else
    echo $1
  fi
}

set-env() {
  echo "::set-env name=$1::$2"
}

set-up-dependencies() {
  local java_version=$1

  set-up-mx
  shallow-clone-graalvm-project https://github.com/oracle/graal.git
  shallow-clone-graalvm-project https://github.com/graalvm/graaljs.git
  download-graalsqueak-image
  ensure-test-image

  if [[ "${java_version}" == "java8" ]]; then
    set-up-openjdk8-jvmci "${HOME}"
  else 
    set-up-labsjdk11 "${HOME}"
  fi

  set-up-graalvm-ce "${java_version}" "${HOME}"

  set-env "INSTALLABLE_TARGET" "$(installable-filename "${java_version}")"
}

set-up-graalvm-ce() {
  local java_version=$1
  local target_dir=$2
  local file_suffix=".tar.gz" && [[ "${OS_NAME}" == "windows" ]]  && file_suffix=".zip"
  local file="graalvm-ce-${java_version}-${OS_NAME}-amd64-${DEP_GRAALVM}${file_suffix}"

  pushd "${target_dir}" > /dev/null

  curl -sSL --retry 3 -o "${file}" "https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-${DEP_GRAALVM}/${file}"
  if [[ "${OS_NAME}" == "windows" ]]; then unzip -qq "${file}"; else tar -xzf "${file}"; fi

  popd > /dev/null

  graalvm_home="${target_dir}/graalvm-ce-${java_version}-${DEP_GRAALVM}${JAVA_HOME_SUFFIX}"
  add-path "${graalvm_home}/bin"
  set-env "GRAALVM_HOME" "$(resolve-path ${graalvm_home})"
  
  echo "[${file} set up successfully]"
}

set-up-labsjdk11() {
  local target_dir=$1
  local jdk_tar=${target_dir}/jdk.tar.gz
  local file="labsjdk-ce-${DEP_JDK11}+${DEP_JDK11_UPDATE}-${DEP_JVMCI}-${OS_NAME}-amd64.tar.gz"

  pushd "${target_dir}" > /dev/null

  curl -sSL --retry 3 -o "${jdk_tar}" "https://github.com/graalvm/labs-openjdk-11/releases/download/${DEP_JVMCI}/${file}"
  tar xzf "${jdk_tar}"

  popd > /dev/null

  enable-jdk "${target_dir}/labsjdk-ce-${DEP_JDK11}-${DEP_JVMCI}${JAVA_HOME_SUFFIX}"

  echo "[${file} set up successfully]"
}

set-up-mx() {
  shallow-clone "https://github.com/graalvm/mx.git" "master" "${HOME}/mx"
  add-path "${HOME}/mx"
  echo "[mx set up successfully]"
}

set-up-openjdk8-jvmci() {
  local target_dir=$1
  local jdk_tar=${target_dir}/jdk.tar.gz
  local file="openjdk-8u${DEP_JDK8_UPDATE}-${DEP_JVMCI}-${OS_NAME}-amd64.tar.gz"

  pushd "${target_dir}" > /dev/null

  curl -sSL --retry 3 -o "${jdk_tar}" "https://github.com/graalvm/openjdk8-jvmci-builder/releases/download/${DEP_JVMCI}/${file}"
  tar xzf "${jdk_tar}"

  popd > /dev/null

  enable-jdk "${target_dir}/openjdk1.8.0_${DEP_JDK8_UPDATE}-${DEP_JVMCI}${JAVA_HOME_SUFFIX}"

  echo "[openjdk1.8.0_${DEP_JDK8_UPDATE}-${DEP_JVMCI} set up successfully]"
}

shallow-clone() {
  local git_url=$1
  local git_commit_or_tag=$2
  local target_dir=$3

  mkdir "${target_dir}" || true
  pushd "${target_dir}" > /dev/null

  git init > /dev/null
  git remote add origin "${git_url}"
  git fetch origin --depth 1 "${git_commit_or_tag}"
  git reset --quiet --hard FETCH_HEAD

  popd > /dev/null
}

shallow-clone-graalvm-project() {
  local git_url=$1
  local name=$(basename "${git_url}" | cut -d. -f1)
  local target_dir="${BASE_DIRECTORY}/../${name}"

  shallow-clone "${git_url}" "vm-${DEP_GRAALVM}" "${target_dir}"
}

$@
