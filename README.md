# GraalSqueak [![Latest Release][graalsqueak_latest_badge]][graalsqueak_latest] [![Slack][graalvm_slack_badge]][graalvm_slack] [![Build Status][travis_badge]][travis] [![Codacy][codacy_grade]][codacy] [![Coverage][codacy_coverage]][codacy] [![License][graalsqueak_license_badge]][graalsqueak_license]

A [Squeak/Smalltalk][squeak] implementation for the [GraalVM][graalvm].


## Getting Started

1. Find the [latest GraalSqueak release][graalsqueak_latest] and identify the
   supported version of GraalVM.
2. Download the corresponding [GraalVM][graalvm_download] for your platform.
3. a) If you are on Linux or macOS, install the pre-compiled GraalSqueak
  component for your platform attached to the release with the GraalVM Updater:

```bash
$GRAALVM_HOME/bin/gu install \
  -u https://github.com/hpi-swa/graalsqueak/releases/download/X.Y.Z/graalsqueak-installable-….jar
```

3. b) If you are on Windows, download the component, change its file extension
  to `.zip`, and unzip it into your GraalVM folder:

```powershell
powershell Expand-Archive graalsqueak-installable-….zip -DestinationPath $GRAALVM_HOME
```

4. You should now be able to open GraalSqueak:

```bash
$GRAALVM_HOME/bin/graalsqueak      # Linux or macOS

$GRAALVM_HOME\bin\graalsqueak.cmd  # Windows
```

## Community Support

If you have a question, need some help, or want to discuss a new feature, feel
free to open an [issue][issues] or join the `#graalsqueak` channel on the
[GraalVM Slack][graalvm_slack].


## Documentation

Documentation is available in [`docs`](docs).


## Development

Active development is done in the [`master` branch][master].
Please feel free to open a [pull request][pull_request] if you'd like to
contribute a bug-fix, documentation, or a new feature.


### Building from Source

The [mx] tool is required to build GraalSqueak.
Running `mx build` in GraalSqueak's root directory checks out all dependencies
and builds all JAR files for running and testing GraalSqueak.
It can also be used to run GraalSqueak via the `mx squeak` command.
As an example, `mx --dy /compiler squeak path/to/a/squeaksmalltalk.image` opens
the image and enables the Graal compiler.
Run `mx squeak --help` and `mx --help` to list all command-line flags you can
run GraalSqueak with.


### Setting Up A New Development Environment

It is recommended to use [Eclipse][eclipse_downloads] with the
[Eclipse Checkstyle Plugin][eclipse_cs] for development.

1. Run `mx eclipseinit` in GraalSqueak's root directory to create all project
   files for Eclipse.
2. Import all projects from the [graal] repository which `mx` should have
   already cloned into the parent directory of your GraalSqueak checkout during
   the build process.
3. Import all projects from GraalSqueak's root directory.
4. Run [`GraalSqueakLauncher`][graalsqueak_launcher] to start GraalSqueak.


## Contributing

Please [report any issues here on GitHub][issues] and open
[pull requests][pull_request] if you'd like to contribute code or documentation.


## Publications

*To cite this work, please use [the GraalSqueak paper presented at MPLR'19][mplr19_paper].*

### 2019
- Fabio Niephaus. [*Smalltalk with the GraalVM*][javaadvent19]. In the
  JVM Programming Advent Calendar, December 7, 2019.
- Fabio Niephaus, Tim Felgentreff, and Robert Hirschfeld. *GraalSqueak: Toward a
Smalltalk-based Tooling Platform for Polyglot Programming*. In Proceedings of
[the International Conference on Managed Programming Languages and Runtimes
(MPLR) 2019][mplr19], co-located with the Conference on Object-oriented
Programming, Systems, Languages, and Applications (OOPSLA), 12 pages, Athens,
Greece, October 21, 2019, ACM DL.  
[![doi][mplr19_doi]][mplr19_paper] [![Preprint][preprint]][mplr19_pdf]
- Daniel Stolpe, Tim Felgentreff, Christian Humer, Fabio Niephaus, and Robert
Hirschfeld. *Language-independent Development Environment Support for Dynamic
Runtimes*. In Proceedings of [the Dynamic Languages Symposium (DLS)
2019][dls19], co-located with the Conference on Object-oriented Programming,
Systems, Languages, and Applications (OOPSLA), 11 pages, Athens, Greece,
October 20, 2019, ACM DL.  
[![doi][dls19_doi]][dls19_paper] [![Preprint][preprint]][dls19_pdf]
- Fabio Niephaus. [*HPI Polyglot Programming Seminar*][pp19_post]. In the
[GraalVM Blog][graalvm_blog], October 11, 2019.
- Fabio Niephaus, Tim Felgentreff, Tobias Pape, and Robert Hirschfeld.
*Efficient Implementation of Smalltalk Activation Records in Language
Implementation Frameworks*. In Proceedings of [the Workshop on Modern Language
Runtimes, Ecosystems, and VMs (MoreVMs) 2019][morevms19], companion volume to
International Conference on the Art, Science, and Engineering of Programming
(‹Programming›), co-located with the International Conference on the Art,
Science, and Engineering of Programming (‹Programming›), 3 pages, Genova, Italy,
April 1, 2019, ACM DL.  
[![doi][morevms19_doi]][morevms19_paper] [![Preprint][preprint]][morevms19_pdf]
- Tobias Pape, Tim Felgentreff, Fabio Niephaus, and Robert Hirschfeld. *Let them
fail: towards VM built-in behavior that falls back to the program*. In
Proceedings of [the Salon des Refusés (SDR) 2019 Workshop][sdr19], companion
volume to International Conference on the Art, Science, and Engineering of
Programming (‹Programming›), co-located with the International Conference on the
Art, Science, and Engineering of Programming (‹Programming›), 7 pages, Genova,
Italy, April 1, 2019, ACM DL.  
[![doi][sdr19_doi]][sdr19_paper] [![Preprint][preprint]][sdr19_pdf]
- Fabio Niephaus, Eva Krebs, Christian Flach, Jens Lincke, and Robert Hirschfeld.
*PolyJuS: A Squeak/Smalltalk-based Polyglot Notebook System for the GraalVM*. In
Proceedings of [the Programming Experience 2019 (PX/19) Workshop][px19],
companion volume to International Conference on the Art, Science, and
Engineering of Programming (‹Programming›), co-located with the International
Conference on the Art, Science, and Engineering of Programming (‹Programming›),
6 pages, Genova, Italy, April 1, 2019, ACM DL.  
[![doi][px19_doi]][px19_paper] [![Preprint][preprint]][px19_pdf]

### 2018
- Fabio Niephaus, Tim Felgentreff, and Robert Hirschfeld. *GraalSqueak: A Fast
Smalltalk Bytecode Interpreter Written in an AST Interpreter Framework.* In
Proceedings of [the Workshop on Implementation, Compilation, Optimization of
Object-Oriented Languages, Programs, and Systems (ICOOOLPS) 2018][icooolps18],
co-located with the European Conference on Object-oriented Programming (ECOOP),
Amsterdam, Netherlands, July 17, 2018, ACM DL.  
[![doi][icooolps18_doi]][icooolps18_paper] [![Preprint][preprint]][icooolps18_pdf]


## License

GraalSqueak is released under the [MIT license][graalsqueak_license].


[codacy]: https://app.codacy.com/gh/hpi-swa/graalsqueak/dashboard
[codacy_coverage]: https://img.shields.io/codacy/coverage/104b3300600346789d604fd269219efe.svg
[codacy_grade]: https://img.shields.io/codacy/grade/104b3300600346789d604fd269219efe.svg
[dls19]: https://conf.researchr.org/home/dls-2019
[dls19_doi]: https://img.shields.io/badge/doi-10.1145/3359619.3359746-blue.svg
[dls19_paper]: https://doi.org/10.1145/3359619.3359746
[dls19_pdf]: https://www.hpi.uni-potsdam.de/hirschfeld/publications/media/StolpeFelgentreffHumerNiephausHirschfeld_2019_LanguageIndependentDevelopmentEnvironmentSupportForDynamicRuntimes_AcmDL.pdf
[eclipse_cs]: http://checkstyle.org/eclipse-cs/
[eclipse_downloads]: https://www.eclipse.org/downloads/
[github_releases]: https://help.github.com/en/github/administering-a-repository/about-releases
[graal]: https://github.com/oracle/graal
[graalsqueak_latest]: https://github.com/hpi-swa/graalsqueak/releases/latest
[graalsqueak_latest_badge]: https://img.shields.io/github/v/release/hpi-swa/graalsqueak
[graalsqueak_launcher]: src/de.hpi.swa.graal.squeak.launcher/src/de/hpi/swa/graal/squeak/launcher/GraalSqueakLauncher.java
[graalsqueak_license]: LICENSE
[graalsqueak_license_badge]: https://img.shields.io/github/license/hpi-swa/graalsqueak
[graalvm]: http://www.graalvm.org/
[graalvm_blog]: https://medium.com/graalvm
[graalvm_download]: http://www.graalvm.org/downloads/
[graalvm_slack]: https://www.graalvm.org/slack-invitation/
[graalvm_slack_badge]: https://img.shields.io/badge/slack-%23graalsqueak-active
[icooolps18]: https://2018.ecoop.org/event/icooolps-2018-papers-graalsqueak-a-fast-smalltalk-bytecode-interpreter-written-in-an-ast-interpreter-framework
[icooolps18_doi]: https://img.shields.io/badge/doi-10.1145/3242947.3242948-blue.svg
[icooolps18_paper]: https://doi.org/10.1145/3242947.3242948
[icooolps18_pdf]: https://fniephaus.com/2018/icooolps18-graalsqueak.pdf
[issues]: ../../issues/new
[javaadvent19]: https://www.javaadvent.com/2019/12/smalltalk-with-the-graalvm.html
[master]: ../../tree/master
[morevms19]: https://2019.programming-conference.org/track/MoreVMs-2019
[morevms19_doi]: https://img.shields.io/badge/doi-10.1145/3328433.3328440-blue.svg
[morevms19_paper]: https://doi.org/10.1145/3328433.3328440
[morevms19_pdf]: https://fniephaus.com/2019/morevms19-efficient-activation-records.pdf
[mplr19]: https://conf.researchr.org/home/mplr-2019
[mplr19_doi]: https://img.shields.io/badge/doi-10.1145/3357390.3361024-blue.svg
[mplr19_paper]: https://doi.org/10.1145/3357390.3361024
[mplr19_pdf]: https://fniephaus.com/2019/mplr19-graalsqueak.pdf
[mx]: https://github.com/graalvm/mx
[pp19_post]: https://medium.com/p/3fd06ffa59d2/
[preprint]: https://img.shields.io/badge/preprint-download-blue.svg
[pull_request]: https://help.github.com/en/github/collaborating-with-issues-and-pull-requests/creating-a-pull-request
[px19]: https://2019.programming-conference.org/track/px-2019-papers
[px19_doi]: https://img.shields.io/badge/doi-10.1145/3328433.3328434-blue.svg
[px19_paper]: https://doi.org/10.1145/3328433.3328434
[px19_pdf]: https://fniephaus.com/2019/px19-polyglot-notebooks.pdf
[sdr19]: https://2019.programming-conference.org/track/sdr-2019-papers
[sdr19_doi]: https://img.shields.io/badge/doi-10.1145/3328433.3338056-blue.svg
[sdr19_paper]: https://doi.org/10.1145/3328433.3338056
[sdr19_pdf]: https://www.hpi.uni-potsdam.de/hirschfeld/publications/media/PapeFelgentreffNiephausHirschfeld_2019_LetThemFailTowardsVmBuiltInBehaviorThatFallsBackToTheProgram_AcmDL.pdf
[squeak]: https://squeak.org
[squeak_downloads]: https://squeak.org/downloads/
[travis]: https://travis-ci.org/hpi-swa/graalsqueak
[travis_badge]: https://travis-ci.org/hpi-swa/graalsqueak.svg?branch=master
