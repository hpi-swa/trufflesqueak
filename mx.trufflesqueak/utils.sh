#!/usr/bin/env bash
#
# Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
# Copyright (c) 2021-2025 Oracle and/or its affiliates
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
  for k, v in suite['trufflesqueak:dependencyMap'].items()]);
graal_version = next(x['version'] for x in suite['imports']['suites'] if x['name'] == 'truffle')
slug = '/'.join(suite['url'].split('/')[-2:]);
mxversion = suite['mxversion']
print('export %s GRAAL_VERSION=%s GITHUB_SLUG=%s MX_VERSION=%s' % (vars, graal_version, slug, mxversion))
END
)
$(cd "${SCRIPT_DIRECTORY}" && python3 -c "${py_export}")
([[ -z "${GRAAL_VERSION}" ]] || [[ -z "${GITHUB_SLUG}" ]]) && \
  echo "Failed to load values from dependencyMap and GitHub slug." 1>&2 && exit 1

OS_NAME=$(uname -s | tr '[:upper:]' '[:lower:]')
[[ "${OS_NAME}" == msys* || "${OS_NAME}" == cygwin* || "${OS_NAME}" == mingw* ]] && OS_NAME="windows"
OS_ARCH="amd64"
[[ "${OS_NAME}" == "linux" ]] && [[ "$(dpkg --print-architecture)" == "arm64" ]] && OS_ARCH="aarch64"
[[ "${OS_NAME}" == "darwin" ]] && [[ "$(uname -m)" == "arm64" ]] && OS_ARCH="aarch64"
JAVA_HOME_SUFFIX="" && [[ "${OS_NAME}" == "darwin" ]] && JAVA_HOME_SUFFIX="/Contents/Home"
readonly OS_NAME OS_ARCH JAVA_HOME_SUFFIX


add-path() {
  echo "$(resolve-path "$1")" >> $GITHUB_PATH
}

build-graalvm() {
  local java_version=$1
  local graalvm_home="$(mx --env trufflesqueak-jar graalvm-home)"

  mx --env trufflesqueak-jar --no-download-progress build --dependencies "GRAALVM_TRUFFLESQUEAK_JAR_JAVA${java_version}"

  add-path "${graalvm_home}/bin"
  set-env "GRAALVM_HOME" "$(resolve-path "${graalvm_home}")"
  echo "[${graalvm_home} set as \$GRAALVM_HOME]"
}

build-standalone() {
  local type=$1
  local java_version=$2
  local component_name=""
  case "${type}" in
    "native")
      component_name="SMALLTALK_NATIVE_STANDALONE_SVM_JAVA${java_version}"
      ;;
    "jvm")
      component_name="SMALLTALK_JAVA_STANDALONE_SVM_JAVA${java_version}"
      ;;
    *)
      echo "Unexpected standalone type: ${type}"
      exit 1
      ;;
  esac
  local env_name="trufflesqueak-${type}"

  mx --env "${env_name}" --no-download-progress build --dependencies "${component_name}"
  cp "$(mx --env "${env_name}" paths "${component_name}")" "${STANDALONE_TARGET}"

  local standalone_home="$(mx --env "${env_name}" standalone-home --type "${type}" smalltalk)"
  add-path "${standalone_home}/bin"
  echo "[${standalone_home}/bin added to \$PATH]"
}

check-deploy() {
  local git_tag=$(git tag --points-at HEAD)
  if [[ -z "${git_tag}" ]]; then
    echo "Skipping deployment step (commit not tagged)"
    exit 0
  elif ! [[ "${git_tag}" =~ ^[[:digit:]] ]]; then
    echo "Skipping deployment step (tag ${git_tag} does not start with a digit)"
    exit 0
  fi
}

conditional-deploy() {
  check-deploy
  eval "$@"
}

deploy-asset() {
  check-deploy
  local filename=$1
  if [[ "${filename}" == *.tar ]]; then
    echo "Compressing tarball..."
    gzip "${filename}"
    filename="${filename}.gz"
  fi
  # zip files are always compressed because mx_trufflesqueak.py forces localCompress

  local auth="Authorization: token $2"
  local git_tag=$(git tag --points-at HEAD)
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

  echo "[TruffleSqueak test image (${DEP_TEST_IMAGE_TAG}) downloaded successfully]"
}

download-cuis-test-image() {
  local target_dir="${BASE_DIRECTORY}/images"

  mkdir "${target_dir}" || true
  pushd "${target_dir}" > /dev/null

  download-asset "${DEP_CUIS_TEST_IMAGE}" "${DEP_CUIS_TEST_IMAGE_TAG}"
  unzip -qq "${DEP_CUIS_TEST_IMAGE}"

  popd > /dev/null

  echo "[Cuis test image (${DEP_CUIS_TEST_IMAGE_TAG}) downloaded successfully]"
}

filename-standalone() {
  local variant="" && [[ "$1" == "jvm" ]] && variant="-jvm"
  local git_describe=$(git describe --tags --always)
  local git_short_commit=$(git log -1 --format="%h")
  local git_description="${git_describe:-${git_short_commit}}"
  local file_extension="tar" && [[ "${OS_NAME}" == "windows" ]] && file_extension="zip"
  echo "trufflesqueak${variant}-${git_description}-${OS_NAME}-${OS_ARCH}.${file_extension}"
}

format-native-image-config() {
  readonly NI_CONFIG_BASE="${BASE_DIRECTORY}/src/de.hpi.swa.trufflesqueak/src/META-INF/native-image"
  for f in "${NI_CONFIG_BASE}/"*.json; do
    underscore --in "${f}" --out "${f}" --wrapwidth 80 print
  done
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
  echo "export $1=\"$2\"" >> "${RUNNER_TEMP}/all_env_vars"
}

set-up-dependencies() {
  local kind=$1
  local java_version=$2

  if [[ "${kind}" == "native" ]]; then
    case "$(uname -s)" in
      "Linux")
        sudo apt-get update --quiet --yes && sudo apt-get install --quiet --yes libz-dev libxi-dev libxtst-dev libxrender-dev libfreetype6-dev
        ;;
    esac
  fi

  # Repository was shallow copied and Git did not fetch tags, so fetch the tag
  # of the commit (if any) to make it available for other Git operations.
  git -c protocol.version=2 fetch --prune --progress --no-recurse-submodules \
    --depth=1 origin "+$(git rev-parse HEAD):refs/remotes/origin/main"

  set-up-mx
  shallow-clone-graal
  set-up-labsjdk "${java_version}"
  download-trufflesqueak-test-image
  if [[ "${kind}" != "jar" ]]; then
    set-env "STANDALONE_TARGET" "$(filename-standalone "${kind}")"
  fi
}

set-up-labsjdk() {
  local java_version=$1
  local jdk_id="labsjdk-ce-${java_version}"
  local target_dir="${RUNNER_TEMP}/jdk"
  local dl_dir="${RUNNER_TEMP}/jdk-dl"
  local mx_suffix="" && [[ "${OS_NAME}" == "windows" ]] && mx_suffix=".cmd"
  mkdir "${dl_dir}"
  pushd "${BASE_DIRECTORY}/../graal/" > /dev/null
  # run fetch-jdk in graal repo, so that it picks up the latest common.json
  "${RUNNER_TEMP}/mx/mx${mx_suffix}" --quiet --java-home= fetch-jdk --jdk-id "${jdk_id}" --to "${dl_dir}" --alias "${target_dir}"
  popd > /dev/null
  enable-jdk "${target_dir}${JAVA_HOME_SUFFIX}"
  echo "[${jdk_id} set up successfully]"
}

set-up-mx() {
  shallow-clone "https://github.com/graalvm/mx.git" "${MX_VERSION}" "${RUNNER_TEMP}/mx"
  add-path "${RUNNER_TEMP}/mx"
  set-env "MX_HOME" "${RUNNER_TEMP}/mx"
  echo "[mx (${MX_VERSION}) set up successfully]"
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
  local git_commit_or_tag=$2
  local name=$(basename "${git_url}" | cut -d. -f1)
  local target_dir="${BASE_DIRECTORY}/../${name}"

  shallow-clone "${git_url}" "${git_commit_or_tag}" "${target_dir}"
}

shallow-clone-graal() {
  shallow-clone-graalvm-project https://github.com/oracle/graal.git "${GRAAL_VERSION}"
  echo "[graal repo (${GRAAL_VERSION}) cloned successfully]"
  $(cd ${BASE_DIRECTORY}/../graal && git apply "${SCRIPT_DIRECTORY}/graalvm-25.0.0.patch")
  echo "[graal repo patched successfully]"
}

eval "$@"
