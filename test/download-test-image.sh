#!/bin/bash
set -e

readonly TARGET_DIR="${TRAVIS_BUILD_DIR}/images"

mkdir "${TARGET_DIR}" > /dev/null
pushd "${TARGET_DIR}"
wget https://www.hpi.uni-potsdam.de/hirschfeld/artifacts/trufflesqueak/Squeak6.0alpha-17606-32bit.zip
unzip Squeak6.0alpha-17606-32bit.zip
mv *.image test.image
mv *.changes test.changes
popd > /dev/null