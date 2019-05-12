#!/usr/bin/env bash
set -e

readonly BASE_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")/../" && pwd)"
readonly LIB_NAME="ffi-test"
readonly OSVM_SUBDIR="platforms/unix/plugins/SqueakFFIPrims/"
readonly TARGET_DIRECTORY="${BASE_DIRECTORY}/lib"
readonly LINUX_TARGET=${TARGET_DIRECTORY}/${LIB_NAME}.so
readonly MACOS_TARGET=${TARGET_DIRECTORY}/${LIB_NAME}.dylib


case "$(uname -s)" in
  "Darwin")
    if [[ -f "${MACOS_TARGET}" ]]; then
      echo "'${MACOS_TARGET}' already exists. Nothing to do."
      exit 0
    fi
    ;;
  "Linux")
    if [[ -f "${LINUX_TARGET}" ]]; then
      echo "'${LINUX_TARGET}' already exists. Nothing to do."
      exit 0
    fi
    ;;
  *)
    echo "Unsupported platform '$(uname -s)'." 1>&2
    exit 1
    ;;
esac

read -p "Provide absolute path to a checkout of OpenSmalltalkVM: " osvm_dir
if [[ ! -d "${osvm_dir}/${OSVM_SUBDIR}" ]]; then
  echo "'${osvm_dir}/${OSVM_SUBDIR}' does not exist." 1>&2
  exit 1
fi


pushd "${osvm_dir}/${OSVM_SUBDIR}" > /dev/null
if [[ ! -f "${LIB_NAME}.c" ]]; then
  echo "'${LIB_NAME}.c' not found." 1>&2
  exit 1
fi

# Ensure $TARGET_DIRECTORY exists
[[ -d "${TARGET_DIRECTORY}" ]] || mkdir "${TARGET_DIRECTORY}"

case "$(uname -s)" in
  "Darwin")
    gcc -c "${LIB_NAME}.c"
    g++ -dynamiclib -undefined suppress -flat_namespace "${LIB_NAME}.o" \
        -o "${MACOS_TARGET}"
    echo "Done. Library is located at '${MACOS_TARGET}'."
    ;;
  "Linux")
    # Build so file
    gcc -shared -o "${LINUX_TARGET}" -fPIC "${LIB_NAME}.c"
    echo "Done. Library is located at '${LINUX_TARGET}'."
    ;;
esac
popd > /dev/null
