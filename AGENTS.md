# TruffleSqueak Agent Reference

## Preflight Checks

Run these before spending time on build failures:

```bash
java --version
mx --version
python3 --version
cmake --version
python3 -c "import re; print(re.search(r'\"version\"\s*:\s*\"(\d+\.\d+\.\d+)', open('mx.trufflesqueak/suite.py').read()).group(1))"
```

The GraalVM version should match `mx.trufflesqueak/suite.py`.

## Setup

Prefer the following manual setup below for local development:

```bash
# Install dependencies (Ubuntu 24.04)
sudo apt-get update
sudo apt-get install -y build-essential zlib1g-dev cmake python3-pip

# Install Oracle GraalVM matching the current suite version.
# If SDKMAN is available, this is the easiest option:
sdk install java <graalvm-version>-graal
sdk use java <graalvm-version>-graal
# Alternatively, use `mx fetch-jdk`.

# Install pre-commit hooks
python3 -m pip install --user pre-commit
pre-commit install

# Work from the parent directory of the TruffleSqueak checkout
export TS_WORKSPACE="$(cd .. && pwd)"

# Clone mx next to trufflesqueak and add it to PATH
git clone https://github.com/graalvm/mx.git "$TS_WORKSPACE/mx"
export PATH="$TS_WORKSPACE/mx:$PATH"

# Clone graal next to trufflesqueak and apply the checked-in patch
mx sforceimport
(cd "$TS_WORKSPACE/graal" && git apply "$TS_WORKSPACE/trufflesqueak/mx.trufflesqueak/graalvm-<graalvm-version>.patch")
```

## Build and Run

Read `mx.trufflesqueak/suite.py` first. It is the quickest way to understand project names, imported suites, and distribution names.

```bash
# Build JVM standalone (faster to build) and find the target build directory
mx --env trufflesqueak-jvm build
mx --env trufflesqueak-jvm paths --output "TRUFFLESQUEAK_JVM_STANDALONE"

# Build native standalone (slower to build) and find the target build directory
mx --env trufflesqueak-native build --build-log important
mx --env trufflesqueak-native paths --output "TRUFFLESQUEAK_NATIVE_STANDALONE"

# Launch TruffleSqueak from the standalone's bin/ directory
trufflesqueak --headless --quiet -- --evaluate "<smalltalk code>"

# Run 10 iterations of the Towers benchmark from AWFY
trufflesqueak --headless --quiet -- --evaluate "AWFYHarness run: #('Towers' 10 1500)"

# Download TruffleSqueak test image
mx.trufflesqueak/utils.sh download-trufflesqueak-test-image

# Run TruffleSqueak tests
trufflesqueak --vm.ea images/test-64bit.image -- src/image-tests/runSqueakTests.st

# Download Cuis test image
mx.trufflesqueak/utils.sh download-cuis-7-5-test-image

# Run Cuis tests
trufflesqueak --vm.ea images/Cuis7.5-7708.image -s src/image-tests/runCuisTests.st
```

## Codebase Map

- `mx.trufflesqueak/suite.py`: suite metadata, external dependencies, project list, and distributions.
- `mx.trufflesqueak/trufflesqueak-jvm`: mx environment for the JVM standalone.
- `mx.trufflesqueak/trufflesqueak-native`: mx environment for the native standalone.
- `src/de.hpi.swa.trufflesqueak.launcher/src/de/hpi/swa/trufflesqueak/launcher/TruffleSqueakLauncher.java`: launcher entry point and CLI behavior.
- `src/de.hpi.swa.trufflesqueak/src/de/hpi/swa/trufflesqueak/image`: image loading, writing, and runtime context.
- `src/de.hpi.swa.trufflesqueak/src/de/hpi/swa/trufflesqueak/model`: object model for Squeak objects.
- `src/de.hpi.swa.trufflesqueak/src/de/hpi/swa/trufflesqueak/nodes`: interpreter, primitives, plugins, dispatch, and process nodes.
- `src/de.hpi.swa.trufflesqueak.ffi.native`: native FFI support built via CMake.
- `src/de.hpi.swa.trufflesqueak.test`: JUnit tests.
- `src/image-tests`: scripts executed inside test images.
- `src/image`: Smalltalk-side sources from the `image` branch submodule.

## Hints

- Generated code lives in `mxbuild/`; never edit files there.
- If `src/image` is empty, run `git submodule update --init --recursive`.
- Use `--vm.ea --vm.agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=8000` to start in debug mode.
- Use `--experimental-options --engine.Compilation=false` to stay on the interpreter.
- Prefer the JVM standalone while exploring or debugging; switch to native only when needed.
