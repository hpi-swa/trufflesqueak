# Development

Active development is done in the [`main` branch][ts_main].
Please feel free to open a [pull request][pull_request] if you'd like to
contribute a bug-fix, documentation, or a new feature.
In addition to the [issues][ts_issues], the [`TODO`s][ts_todos] and the
[test map][ts_test_map] are good starting points if you'd like to contribute to
TruffleSqueak.


## Building from Source

```bash
mkdir trufflesqueak-build
cd trufflesqueak-build
git clone https://github.com/graalvm/mx
git clone https://github.com/hpi-swa/trufflesqueak
cd trufflesqueak
echo "JAVA_HOME=/path/to/a/jvmci-enabled-JDK" > mx.trufflesqueak/env
../mx/mx --env trufflesqueak-jvm build
export GRAALVM_HOME=$(../mx/mx --env trufflesqueak-jvm graalvm-home)
$GRAALVM_HOME/bin/trufflesqueak path/to/a/squeaksmalltalk.image

../mx/mx --help
```

It is recommended to create a dedicated directory (named `trufflesqueak-build`
in the above) to develop and build TruffleSqueak.
TruffleSqueak requires the [mx] build tool, which will clone all dependencies
(e.g. Truffle and the Graal compiler) in the parent directory of your local
TruffleSqueak checkout.
Using the `trufflesqueak-jvm` environment file ensures that the Graal compiler
is built with TruffleSqueak and enabled by default.
For this, however, you must use a JVMCI-enabled JDK, such as the
[JDK11-based LabsJDK][labsjdk]. Use `mx fetch-sdk` to download the LabsJDK.

## Development With Other GraalVM Languages

If you need access to other GraalVM languages during development, there are one option:

You build TruffleSqueak and other languages from source. For this, please refer to the [instructions for building custom GraalVM distributions][graalvm_vm_readme]. As an example, here is how to build a GraalVM with the Graal compiler, TruffleSqueak and GraalVM's Node.js runtime:

```bash
cd trufflesqueak-build/
git clone git@github.com:oracle/graaljs.git # Clone graal language repository
cd graal/vm # Change to the GraalVM folder
mx --dy trufflesqueak,/graal-nodejs,/compiler build
export GRAALVM_HOME=$(mx --dy trufflesqueak,/graal-nodejs,/compiler graalvm-home)
$GRAALVM_HOME/bin/trufflesqueak path/to/a/squeaksmalltalk.image
```

## Setting Up A New Development Environment

It is recommended to use [Eclipse][eclipse_downloads] with the
[Eclipse Checkstyle Plugin][eclipse_cs] for development, but note that [mx] also
supports IntelliJ and NetBeans.

1. Run `mx eclipseinit` in TruffleSqueak's root directory to create all project
   files for Eclipse. You can also use `mx intellijinit` or any other IDE setup command in `mx`.
2. Import all projects from the [graal] repository which `mx` should have
   already cloned into the parent directory of your TruffleSqueak checkout during
   the build process.
3. Import all projects from TruffleSqueak's root directory.
4. Run [`TruffleSqueakLauncher`][ts_launcher] to start TruffleSqueak, or
   continue to use `mx` to build and run it.


## Testing

TruffleSqueak is continuously tested on different platforms and in different
settings [using GitHub actions][ts_gha].
You may find the [`ci.yml`][ts_ci] interesting.
As part of the `SqueakSUnitTest` test case, TruffleSqueak runs the tests of a
test image and checks the results against [this test map][ts_test_map].


## Debugging

To start TruffleSqueak in debug mode, you can run the following and connect your
preferred Java debugger to the process:
```bash
$GRAALVM_HOME/bin/trufflesqueak \
   --vm.agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000 \
   your.image
```

The following runs the `ArrayTest>>testAtWrap` SUnit test from Squeak/Smalltalk
using mx's unittest command in debug mode:
```bash
mx -d --env trufflesqueak-jvm unittest \
   -DsqueakTests="ArrayTest>>testAtWrap" \
   SqueakSUnitTest --very-verbose --enable-timing --color
```

If you want to debug an installed TruffleSqueak component, you can run the
following:
```bash
trufflesqueak --vm.Xrunjdwp:transport=dt_socket,server=y,address=8000,suspend=y your.image
```

To list additional command-line flags for debugging, run:
```bash
trufflesqueak --help
trufflesqueak --help:languages
trufflesqueak --help:languages --help:internal
```

The following, for example, runs TruffleSqueak in headless mode executing
`1 tinyBenchmarks` with compilation traces enabled and dumps Truffle and Graal
graphs for further inspection with the [Ideal Graph Visualizer (igv)][igv].

```bash
trufflesqueak --code "1 tinyBenchmarks" --engine.TraceCompilation --vm.Dgraal.Dump=Truffle:1
```

Furthermore, TruffleSqueak comes with various infrastructures for different
debugging and tracing purposes (e.g. [`DebugUtils`][ts_debug_utils] and
[loggers][ts_log_utils]).
You may also find the [Truffle docs][truffle_docs] useful.
For additional help, feel free to join the `#trufflesqueak` channel on the
[GraalVM Slack][graalvm_slack].


[eclipse_cs]: https://checkstyle.org/eclipse-cs/
[eclipse_downloads]: https://www.eclipse.org/downloads/
[graal]: https://github.com/oracle/graal
[graalvm_dev_build]: https://github.com/graalvm/graalvm-ce-dev-builds/releases/latest
[graalvm_slack]: https://www.graalvm.org/slack-invitation/
[graalvm_vm_readme]: https://github.com/oracle/graal/blob/master/vm/README.md
[igv]: https://docs.oracle.com/en/graalvm/enterprise/20/guide/tools/ideal-graph-visualizer.html
[labsjdk]: https://github.com/graalvm/labs-openjdk-11/releases
[mx]: https://github.com/graalvm/mx
[pull_request]: https://help.github.com/en/github/collaborating-with-issues-and-pull-requests/creating-a-pull-request
[truffle_docs]: https://github.com/oracle/graal/tree/master/truffle/docs
[ts_ci]: https://github.com/hpi-swa/trufflesqueak/blob/main/.github/workflows/ci.yml
[ts_debug_utils]: https://github.com/hpi-swa/trufflesqueak/blob/main/src/de.hpi.swa.trufflesqueak/src/de/hpi/swa/trufflesqueak/util/DebugUtils.java
[ts_gha]: https://github.com/hpi-swa/trufflesqueak/actions
[ts_issues]: https://github.com/hpi-swa/trufflesqueak/issues
[ts_launcher]: https://github.com/hpi-swa/trufflesqueak/blob/main/src/de.hpi.swa.trufflesqueak.launcher/src/de/hpi/swa/trufflesqueak/launcher/TruffleSqueakLauncher.java
[ts_log_utils]: https://github.com/hpi-swa/trufflesqueak/blob/main/src/de.hpi.swa.trufflesqueak/src/de/hpi/swa/trufflesqueak/util/LogUtils.java
[ts_main]: https://github.com/hpi-swa/trufflesqueak/tree/main
[ts_test_map]: https://github.com/hpi-swa/trufflesqueak/blob/main/src/de.hpi.swa.trufflesqueak.test/src/de/hpi/swa/trufflesqueak/test/tests.properties
[ts_todos]: https://github.com/hpi-swa/trufflesqueak/search?q=%22TODO%22
