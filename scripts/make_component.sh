#!/usr/bin/env bash
set -e

readonly GRAALVM_VERSION="1.0.0-rc16"

readonly BASE_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
readonly COMPONENT_DIR="component_temp_dir"
readonly GRAALSQUEAK_DIR="${BASE_DIR}/.."
readonly GRAALSQUEAK_JAR="${GRAALSQUEAK_DIR}/graalsqueak.jar"
readonly LANGUAGE_PATH="${COMPONENT_DIR}/jre/languages/squeaksmalltalk"
readonly LIB_GRAALVM_PATH="${COMPONENT_DIR}/jre/lib/graalvm"
readonly MANIFEST="${COMPONENT_DIR}/META-INF/MANIFEST.MF"
readonly TARGET_JAR="${GRAALSQUEAK_DIR}/graalsqueak-component.jar"
readonly TEMPLATE_LAUNCHER="template.graalsqueak.sh"

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
cp "${BASE_DIR}/template.graalsqueak.sh" "${LANGUAGE_PATH}/bin/graalsqueak"
cp "${GRAALSQUEAK_DIR}/graalsqueak-launcher.jar" "$LIB_GRAALVM_PATH"

mkdir -p "${COMPONENT_DIR}/META-INF"

touch "${MANIFEST}"
echo "Bundle-Name: GraalSqueak" >> "${MANIFEST}"
echo "Bundle-Symbolic-Name: de.hpi.swa.graal.squeak" >> "${MANIFEST}"
echo "Bundle-Version: ${GRAALVM_VERSION}" >> "${MANIFEST}"
echo "Bundle-RequireCapability: org.graalvm; filter:=\"(&(graalvm_version=${GRAALVM_VERSION})(os_arch=amd64))\"" >> "${MANIFEST}"
echo "x-GraalVM-Polyglot-Part: True" >> "${MANIFEST}"

pushd "${COMPONENT_DIR}" > /dev/null
jar cfm "${TARGET_JAR}" META-INF/MANIFEST.MF .

echo "bin/graalsqueak = ../jre/languages/squeaksmalltalk/bin/graalsqueak" > META-INF/symlinks
jar uf "${TARGET_JAR}" META-INF/symlinks

echo "jre/languages/squeaksmalltalk/bin/graalsqueak = rwxrwxr-x" > META-INF/permissions
jar uf "${TARGET_JAR}" META-INF/permissions
popd > /dev/null
rm -rf "${COMPONENT_DIR}"

echo "SUCCESS! The component is located at '${TARGET_JAR}'."
