#!/usr/bin/env bash
#
# Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
#
# Licensed under the MIT License.
#

readonly GRAALVM_VERSION="19.3.0"
readonly JVMCI_VERSION="jvmci-19.3-b05"
readonly JDK8_UPDATE_VERSION="232"

readonly DEFAULT_GIT_TAG="1.0.0-rc5"
readonly IMAGE64_NAME="GraalSqueakTestImage-19230-64bit.zip"
readonly GITHUB_SLUG="hpi-swa/graalsqueak"

OS_NAME=$(uname -s | tr '[:upper:]' '[:lower:]')
[[ "${OS_NAME}" == msys* || "${OS_NAME}" == cygwin* || "${OS_NAME}" == mingw* ]] && export OS_NAME="windows"
JAVA_HOME_SUFFIX="" && [[ "${OS_NAME}" == "darwin" ]] && JAVA_HOME_SUFFIX="/Contents/Home"
readonly OS_NAME JAVA_HOME_SUFFIX


download_assert() {
  local filename=$1
  local git_tag="${2:-${DEFAULT_GIT_TAG}}"
  local target="${3:-$1}"
  curl -L --retry 3 --retry-connrefused --retry-delay 2 -o "${target}" \
    "https://github.com/${GITHUB_SLUG}/releases/download/${git_tag}/${filename}"
}

download_graalvm_ce() {
  local target_dir=$1
  local file_suffix=".tar.gz" && [[ "${OS_NAME}" == "windows" ]]  && file_suffix=".zip"
  local file="graalvm-ce-java8-${OS_NAME}-amd64-${GRAALVM_VERSION}${file_suffix}"

  pushd "${target_dir}" > /dev/null

  curl -sSL --retry 3 -o ${file} https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-${GRAALVM_VERSION}/${file}
  if [[ "${OS_NAME}" == "windows" ]]; then unzip -qq ${file}; else tar -xzf ${file}; fi
  echo "$(pwd)/graalvm-ce-java8-${GRAALVM_VERSION}${JAVA_HOME_SUFFIX}"

  popd > /dev/null
}

download_openjdk8_jvmci() {
  local target_dir=$1
  local jdk_tar=${target_dir}/jdk.tar.gz

  pushd "${target_dir}" > /dev/null

  curl -sSL --retry 3 -o ${jdk_tar} https://github.com/graalvm/openjdk8-jvmci-builder/releases/download/${JVMCI_VERSION}/openjdk-8u${JDK8_UPDATE_VERSION}-${JVMCI_VERSION}-${OS_NAME}-amd64.tar.gz
  tar xzf ${jdk_tar}
  echo "$(pwd)/openjdk1.8.0_${JDK8_UPDATE_VERSION}-${JVMCI_VERSION}${JAVA_HOME_SUFFIX}"

  popd > /dev/null
}

ensure_test_image_64bit() {
  local target_dir=$1

  if [[ -f "${target_dir}/test-64bit.image" ]]; then
    return
  fi

  mkdir "${target_dir}" || true
  pushd "${target_dir}" > /dev/null

  download_assert "${IMAGE64_NAME}"
  unzip "${IMAGE64_NAME}"
  mv ./*.image test-64bit.image
  mv ./*.changes test-64bit.changes

  popd > /dev/null
}

shallow_clone() {
  local git_url=$1
  local git_commit_or_tag=$2
  local target_dir=$3

  mkdir ${target_dir} || true
  pushd ${target_dir} > /dev/null

  git init > /dev/null
  git remote add origin "${git_url}"
  git fetch origin --depth 1 "${git_commit_or_tag}"
  git reset --hard FETCH_HEAD

  popd > /dev/null
}
