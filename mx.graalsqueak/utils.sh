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

download-asset() {
  local filename=$1
  local git_tag=$2
  local target="${3:-$1}"

  curl -L --retry 3 --retry-connrefused --retry-delay 2 -o "${target}" \
    "https://github.com/${GITHUB_SLUG}/releases/download/${git_tag}/${filename}"
}

download-graalvm-ce() {
  local target_dir=$1
  local file_suffix=".tar.gz" && [[ "${OS_NAME}" == "windows" ]]  && file_suffix=".zip"
  local file="graalvm-ce-java8-${OS_NAME}-amd64-${DEP_GRAALVM}${file_suffix}"

  pushd "${target_dir}" > /dev/null

  curl -sSL --retry 3 -o "${file}" "https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-${DEP_GRAALVM}/${file}"
  if [[ "${OS_NAME}" == "windows" ]]; then unzip -qq "${file}"; else tar -xzf "${file}"; fi
  echo "$(pwd)/graalvm-ce-java8-${DEP_GRAALVM}${JAVA_HOME_SUFFIX}"

  popd > /dev/null
}

download-openjdk8-jvmci() {
  local target_dir=$1
  local jdk_tar=${target_dir}/jdk.tar.gz

  pushd "${target_dir}" > /dev/null

  curl -sSL --retry 3 -o "${jdk_tar}" "https://github.com/graalvm/openjdk8-jvmci-builder/releases/download/${DEP_JVMCI}/openjdk-8u${DEP_JDK8_UPDATE}-${DEP_JVMCI}-${OS_NAME}-amd64.tar.gz"
  tar xzf "${jdk_tar}"
  echo "$(pwd)/openjdk1.8.0_${DEP_JDK8_UPDATE}-${DEP_JVMCI}${JAVA_HOME_SUFFIX}"

  popd > /dev/null
}

download-graalsqueak-image() {
  local target_dir="${BASE_DIRECTORY}/src/resources"

  pushd "${target_dir}" > /dev/null

  download-asset "${DEP_IMAGE}" "${DEP_IMAGE_TAG}"
  unzip "${DEP_IMAGE}"
  rm -f "${DEP_IMAGE}"

  popd > /dev/null
}

ensure-test-image() {
  local target_dir="${BASE_DIRECTORY}/images"

  if [[ -f "${target_dir}/test-64bit.image" ]]; then
    return
  fi

  mkdir "${target_dir}" || true
  pushd "${target_dir}" > /dev/null

  download-asset "${DEP_TEST_IMAGE}" "${DEP_TEST_IMAGE_TAG}"
  unzip "${DEP_TEST_IMAGE}"
  mv ./*.image test-64bit.image
  mv ./*.changes test-64bit.changes

  popd > /dev/null
}

installable-filename() {
  local git_describe=$(git describe --tags)
  local git_short_commit=$(git log -1 --format="%h")
  local git_description="${git_describe:-${git_short_commit}}"
  echo "graalsqueak-installable-${OS_NAME}-amd64-${git_description}-for-GraalVM-${DEP_GRAALVM}.jar"
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
