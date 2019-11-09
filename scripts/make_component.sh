#!/usr/bin/env bash
#
# Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
#
# Licensed under the MIT License.
#

set -e

readonly LANGUAGE_ID="smalltalk"
readonly GRAALVM_VERSION="19.2.1"
readonly OS_NAME=$(uname -s | tr '[:upper:]' '[:lower:]')
case "${OS_NAME}" in
    "darwin")
        OS_NAME_FOR_GU=macos
        ;;
    *)
        OS_NAME_FOR_GU="${OS_NAME}"
        ;;
esac
readonly OS_NAME_FOR_GU
readonly OS_ARCH_FOR_GU="amd64"
readonly GIT_DESCRIPTION=$(git describe --tags)

readonly BASE_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
readonly COMPONENT_DIR="component_temp_dir"
readonly GRAALSQUEAK_DIR="${BASE_DIR}/.."
readonly GRAALSQUEAK_JAR="${GRAALSQUEAK_DIR}/graalsqueak.jar"
readonly LANGUAGE_PATH="${COMPONENT_DIR}/jre/languages/${LANGUAGE_ID}"
readonly LIB_GRAALVM_PATH="${COMPONENT_DIR}/jre/lib/graalvm"
readonly MANIFEST="${COMPONENT_DIR}/META-INF/MANIFEST.MF"
readonly RELEASE_FILE="${LANGUAGE_PATH}/release"
readonly TARGET_JAR="${GRAALSQUEAK_DIR}/graalsqueak-installable-${OS_NAME}-${OS_ARCH_FOR_GU}-${GIT_DESCRIPTION}-for-GraalVM-${GRAALVM_VERSION}.jar"
readonly TEMPLATE_LAUNCHER="template.graalsqueak.sh"
readonly TEMPLATE_WIN_LAUNCHER="template.graalsqueak.cmd"

if [[ -d "${COMPONENT_DIR}" ]]; then
    read -p "'${COMPONENT_DIR}' already exists. Do you want to remove it? (y/N): " user_input
    if [[ "${user_input}" != "y" ]]; then
        exit 0
    fi
    rm -rf "${COMPONENT_DIR}"
fi

if [[ ! -f "${GRAALSQUEAK_JAR}" ]]; then
    echo "Could not find '${GRAALSQUEAK_JAR}'. Did you run 'mx build'?"
    exit 1
fi

mkdir -p "${LANGUAGE_PATH}" "${LANGUAGE_PATH}/bin" "${LIB_GRAALVM_PATH}"
cp "${GRAALSQUEAK_DIR}/graalsqueak.jar" \
	"${GRAALSQUEAK_DIR}/graalsqueak-shared.jar" \
    "${LANGUAGE_PATH}"
cp "${BASE_DIR}/${TEMPLATE_LAUNCHER}" "${LANGUAGE_PATH}/bin/graalsqueak"
cp "${BASE_DIR}/${TEMPLATE_WIN_LAUNCHER}" "${LANGUAGE_PATH}/bin/graalsqueak.cmd"
cp "${GRAALSQUEAK_DIR}/graalsqueak-launcher.jar" "$LIB_GRAALVM_PATH"
cp "${GRAALSQUEAK_DIR}/LICENSE" "${COMPONENT_DIR}/LICENSE_GRAALSQUEAK.txt"

mkdir -p "${COMPONENT_DIR}/META-INF"

touch "${MANIFEST}"
echo "Bundle-Name: GraalSqueak" >> "${MANIFEST}"
echo "Bundle-Symbolic-Name: de.hpi.swa.graal.squeak" >> "${MANIFEST}"
echo "Bundle-Version: ${GRAALVM_VERSION}" >> "${MANIFEST}"
echo "Bundle-RequireCapability: org.graalvm; filter:=\"(&(graalvm_version=${GRAALVM_VERSION})(os_name=${OS_NAME_FOR_GU})(os_arch=${OS_ARCH_FOR_GU}))\"" >> "${MANIFEST}"
echo "x-GraalVM-Polyglot-Part: True" >> "${MANIFEST}"

readonly GIT_HASH="$(git rev-parse HEAD)"
readonly GIT_BRANCH_NAME="$(git rev-parse --abbrev-ref HEAD)"
readonly GIT_COMMITTER_NAME="$(git config user.name)"
readonly GIT_COMMITTER_EMAIL="$(git config user.email)"

echo "OS_NAME=${OS_NAME_FOR_GU}" > "${RELEASE_FILE}"
echo "OS_ARCH=${OS_ARCH_FOR_GU}" >> "${RELEASE_FILE}"
echo "SOURCE=\"${GIT_BRANCH_NAME}:${GIT_HASH}\"" >> "${RELEASE_FILE}"
echo "COMMIT_INFO={\"${GIT_BRANCH_NAME}\": {\"commit.committer\": \"${GIT_COMMITTER_NAME} <${GIT_COMMITTER_EMAIL}>\", \"commit.rev\": \"${GIT_HASH}\"}}" >> "${RELEASE_FILE}"
echo "GRAALVM_VERSION=${GRAALVM_VERSION}" >> "${RELEASE_FILE}"
## echo "component_catalog=..." >> "${RELEASE_FILE}"

pushd "${COMPONENT_DIR}" > /dev/null
jar cfm "${TARGET_JAR}" META-INF/MANIFEST.MF .

echo "bin/graalsqueak = ../jre/bin/graalsqueak" > META-INF/symlinks
echo "jre/bin/graalsqueak = ../languages/${LANGUAGE_ID}/bin/graalsqueak" >> META-INF/symlinks
jar uf "${TARGET_JAR}" META-INF/symlinks

echo "jre/languages/${LANGUAGE_ID}/bin/graalsqueak = rwxrwxr-x" > META-INF/permissions
jar uf "${TARGET_JAR}" META-INF/permissions
popd > /dev/null
rm -rf "${COMPONENT_DIR}"

echo "SUCCESS! The component is located at '${TARGET_JAR}'."
