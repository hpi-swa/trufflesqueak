#!/usr/bin/env bash
#
# Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
#
# Licensed under the MIT License.
#

set -o errexit
set -o errtrace
set -o pipefail
set -o nounset

readonly SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")/" && pwd)"
readonly BASE_DIRECTORY="$(dirname "${SCRIPT_DIRECTORY}")"
readonly ROOT_DIRECTORY="$(dirname "${BASE_DIRECTORY}")"
readonly GRAAL_DIRECTORY="${ROOT_DIRECTORY}/graal"
readonly JDK_DIRECTORY="${HOME}/jdk"
readonly MX_DIRECTORY="${HOME}/mx"

# Load metadata from suite.py
readonly py_export=$(cat <<-END
from suite import suite;
vars= ' '.join(['DEP_%s=%s' % (k.upper(), v)
  for k, v in suite['trufflesqueak:dependencyMap'].items()]);
slug = '/'.join(suite['url'].split('/')[-2:]);
print('export %s GITHUB_SLUG=%s' % (vars, slug))
END
)
$(cd "${SCRIPT_DIRECTORY}" && python -c "${py_export}")
([[ -z "${DEP_GRAALVM}" ]] || [[ -z "${GITHUB_SLUG}" ]]) && \
  echo "Failed to load values from dependencyMap and GitHub slug." 1>&2 && exit 1

OS_NAME=$(uname -s | tr '[:upper:]' '[:lower:]')
[[ "${OS_NAME}" == msys* || "${OS_NAME}" == cygwin* || "${OS_NAME}" == mingw* ]] && OS_NAME="windows"
OS_ARCH="amd64"
[[ "${OS_NAME}" == "linux" ]] && [[ "$(dpkg --print-architecture)" == "arm64" ]] && OS_ARCH="aarch64"
JAVA_HOME_SUFFIX="" && [[ "${OS_NAME}" == "darwin" ]] && JAVA_HOME_SUFFIX="/Contents/Home"
readonly OS_NAME OS_ARCH JAVA_HOME_SUFFIX


add-path() {
  echo "$(resolve-path "$1")" >> $GITHUB_PATH
}

build-component() {
  local component_name=$1
  local env_name=$2
  local target=$3

  mx --env "${env_name}" build --dependencies="${component_name}"
  cp $(mx --env "${env_name}" paths "${component_name}") "${target}"
}

build-components() {
  local java_version=$1

  if [[ "${java_version}" == "java8" ]]; then
    java_version="JAVA8"
  else
    java_version="JAVA11"
  fi

  mx build # Ensure all dependencies are built (e.g. GRAAL_SDK)

  build-component "SMALLTALK_INSTALLABLE_${java_version}" trufflesqueak-jvm "${INSTALLABLE_JVM_TARGET}"
  build-component "SMALLTALK_INSTALLABLE_SVM_${java_version}" trufflesqueak-svm "${INSTALLABLE_SVM_TARGET}"
}

deploy-asset() {
  local git_tag=$(git tag --points-at HEAD)
  if [[ -z "${git_tag}" ]]; then
    echo "Skipping deployment step (commit not tagged)"
    exit 0
  elif ! [[ "${git_tag}" =~ ^[[:digit:]] ]]; then
    echo "Skipping deployment step (tag ${git_tag} does not start with a digit)"
    exit 0
  fi
  local filename=$1
  local auth="Authorization: token $2"
  local release_id

  tag_result=$(curl -L --retry 3 --retry-connrefused --retry-delay 2 -sH "${auth}" \
    "https://api.github.com/repos/${GITHUB_SLUG}/releases/tags/${git_tag}")
  
  if echo "${tag_result}" | grep -q '"id":'; then
    release_id=$(echo "${tag_result}" | grep '"id":' | head -n 1 | sed 's/[^0-9]*//g')
    echo "Found GitHub release #${release_id} for ${git_tag}"
  else
    # Retry (in case release was just created by some other worker)
    tag_result=$(curl -L --retry 3 --retry-connrefused --retry-delay 2 -sH "${auth}" \
    "https://api.github.com/repos/${GITHUB_SLUG}/releases/tags/${git_tag}")
  
    if echo "${tag_result}" | grep -q '"id":'; then
      release_id=$(echo "${tag_result}" | grep '"id":' | head -n 1 | sed 's/[^0-9]*//g')
      echo "Found GitHub release #${release_id} for ${git_tag}"
    else
      create_result=$(curl -sH "${auth}" \
        --data "{\"tag_name\": \"${git_tag}\",
                \"name\": \"${git_tag}\",
                \"body\": \"\",
                \"draft\": false,
                \"prerelease\": false}" \
        "https://api.github.com/repos/${GITHUB_SLUG}/releases")
      if echo "${create_result}" | grep -q '"id":'; then
        release_id=$(echo "${create_result}" | grep '"id":' | head -n 1 | sed 's/[^0-9]*//g')
        echo "Created GitHub release #${release_id} for ${git_tag}"
      else
        echo "Failed to create GitHub release for ${git_tag}"
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

download-trufflesqueak-image() {
  local target_dir="${BASE_DIRECTORY}/src/resources"

  if ls -1 ${target_dir}/*.image 2>/dev/null; then
    echo "[TruffleSqueak image already downloaded]"
    return
  fi

  pushd "${target_dir}" > /dev/null

  download-asset "${DEP_IMAGE}" "${DEP_IMAGE_TAG}"
  unzip -qq "${DEP_IMAGE}"
  rm -f "${DEP_IMAGE}"

  popd > /dev/null

  echo "[TruffleSqueak image downloaded successfully]"
}

enable-jdk() {
  add-path "$1/bin"
  set-env "JAVA_HOME" "$(resolve-path "$1")"
}

download-trufflesqueak-test-image() {
  local target_dir="${BASE_DIRECTORY}/images"

  if [[ -f "${target_dir}/test-64bit.image" ]]; then
    echo "[TruffleSqueak test image already downloaded]"
    return
  fi

  mkdir "${target_dir}" || true
  pushd "${target_dir}" > /dev/null

  download-asset "${DEP_TEST_IMAGE}" "${DEP_TEST_IMAGE_TAG}"
  unzip -qq "${DEP_TEST_IMAGE}"
  mv ./*.image test-64bit.image
  mv ./*.changes test-64bit.changes

  popd > /dev/null

  echo "[TruffleSqueak test image downloaded successfully]"
}

installable-filename() {
  local java_version=$1
  local svm_prefix=$2
  local git_describe=$(git describe --tags --always)
  local git_short_commit=$(git log -1 --format="%h")
  local git_description="${git_describe:-${git_short_commit}}"
  echo "trufflesqueak-installable${svm_prefix}-${java_version}-${OS_NAME}-${OS_ARCH}-${git_description}.jar"
}

resolve-path() {
  if [[ "${OS_NAME}" == "windows" ]]; then
    # Convert Unix path to Windows path
    echo "$1" | sed 's/\/c/C:/g' | sed 's/\//\\/g'
  else
    echo "$1"
  fi
}

set-env() {
  echo "$1=$2" >> $GITHUB_ENV
  echo "export $1=\"$2\"" >> "${HOME}/all_env_vars"
}

set-up-dependencies() {
  local java_version=$1

  # Repository was shallow copied and Git did not fetch tags, so fetch the tag
  # of the commit (if any) to make it available for other Git operations.
  git -c protocol.version=2 fetch --prune --progress --no-recurse-submodules \
    --depth=1 origin "+$(git rev-parse HEAD):refs/remotes/origin/master"

  set-up-mx
  shallow-clone-graalvm-project https://github.com/oracle/graal.git
  shallow-clone-graalvm-project https://github.com/graalvm/graaljs.git
  download-trufflesqueak-image
  download-trufflesqueak-test-image
  set-up-jdk "${java_version}" "${JDK_DIRECTORY}"
  set-up-graalvm-ce "${java_version}" "${HOME}"
  set-env "INSTALLABLE_JVM_TARGET" "$(installable-filename "${java_version}" "")"
  set-env "INSTALLABLE_SVM_TARGET" "$(installable-filename "${java_version}" "-svm")"
}

set-up-graalvm-ce() {
  local java_version=$1
  local target_dir=$2
  local graalvm_name="graalvm-ce-${java_version}-${OS_NAME}-${OS_ARCH}-${DEP_GRAALVM}"
  local file_suffix=".tar.gz" && [[ "${OS_NAME}" == "windows" ]] && file_suffix=".zip"
  local file="${graalvm_name}${file_suffix}"

  pushd "${target_dir}" > /dev/null

  curl -sSL --retry 3 -o "${file}" "https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-${DEP_GRAALVM}/${file}"
  if [[ "${OS_NAME}" == "windows" ]]; then unzip -qq "${file}"; else tar -xzf "${file}"; fi

  popd > /dev/null

  graalvm_home="${target_dir}/graalvm-ce-${java_version}-${DEP_GRAALVM}${JAVA_HOME_SUFFIX}"
  add-path "${graalvm_home}/bin"
  set-env "GRAALVM_HOME" "$(resolve-path "${graalvm_home}")"
  
  echo "[${graalvm_name} set up successfully]"
}

set-up-jdk()  {
  local java_version=$1
  local target_dir=$2
  local jdk="labsjdk-ce-11" && [[ "${java_version}" == "java8" ]] && jdk="openjdk8"
  ${MX_DIRECTORY}/mx fetch-jdk \
    --configuration "${GRAAL_DIRECTORY}/common.json" \
    --java-distribution "${jdk}" --to "${target_dir}" --alias "${JAVA_HOME}"
  echo "[${jdk} set up successfully]"
}

set-up-mx() {
  shallow-clone "https://github.com/graalvm/mx.git" "master" "${MX_DIRECTORY}"
  add-path "${MX_DIRECTORY}"
  set-env "MX_HOME" "${MX_DIRECTORY}"
  echo "[mx set up successfully]"
}

shallow-clone() {
  local git_url=$1
  local git_commit_or_tag=$2
  local target_dir=$3

  mkdir "${target_dir}" || true
  pushd "${target_dir}" > /dev/null

  git init > /dev/null
  git remote add origin "${git_url}"
  git fetch --quiet --depth 1 origin "${git_commit_or_tag}"
  git reset --quiet --hard FETCH_HEAD

  popd > /dev/null
}

shallow-clone-graalvm-project() {
  local git_url=$1
  local name=$(basename "${git_url}" | cut -d. -f1)
  local target_dir="${ROOT_DIRECTORY}/${name}"

  shallow-clone "${git_url}" "vm-${DEP_GRAALVM}" "${target_dir}"
}

$@
