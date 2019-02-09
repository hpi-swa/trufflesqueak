# GraalSqueak [![Build Status][travis_badge]][travis] [![Codacy][codacy_grade]][codacy] [![Coverage][codacy_coverage]][codacy]

A [Squeak/Smalltalk][squeak] implementation for the [GraalVM][graalvm].


## Getting Started

1. Clone or download GraalSqueak.
2. Download the latest [Oracle Labs JDK][labsjdk] for your platform.
3. Ensure your `JAVA_HOME` environment variable is unset or set it to
   Oracle Labs JDK's home directory.
4. Run [`bin/graalsqueak`][graalsqueak] to build and start GraalSqueak with
   [mx] using the Oracle Labs JDK.
5. Open a recent [Squeak/Smalltalk image][squeak_downloads] (Squeak-5.2 or later).

To list all available options, run `bin/graalsqueak -h`.


## Development

Active development is done on the [`dev` branch][dev] and new code is merged to
[`master`][master] when stable.
Therefore, please open pull requests against [`dev`][dev] if you like to
contribute a bugfix or a new feature.


### Setting Up A New Development Environment

It is recommended to use [Eclipse][eclipse_downloads] with the
[Eclipse Checkstyle Plugin][eclipse_cs] for development.

1. Run `mx eclipseinit` in GraalSqueak's root directory to create all project
   files for Eclipse.
2. Import all projects from the [graal] repository which `mx` should have
   already cloned into the parent directory of your GraalSqueak checkout during
   the build process.
3. Import all projects from GraalSqueak's root directory.
4. Launch [`GraalSqueakLauncher`][graalsqueak_launcher] to start GraalSqueak.

Run `mx --help` and `mx squeak --help` to list all commands that may help you
develop GraalSqueak.


## Contributing

Please [report any issues here on GitHub][issues] and open
[pull requests][pull_request] if you'd like to contribute code or documentation.


## Papers

- Fabio Niephaus, Tim Felgentreff, and Robert Hirschfeld. *GraalSqueak: A Fast
Smalltalk Bytecode Interpreter Written in an AST Interpreter Framework.* In
Proceedings of [the Workshop on Implementation, Compilation, Optimization of
Object-Oriented Languages, Programs and Systems (ICOOOLPS) 2018][icooolps18],
co-located with [the European Conference on Object-oriented Programming
(ECOOP)][ecoop], Amsterdam, Netherlands, July 17, 2018, ACM DL.  
   [![doi][icooolps18_doi]][icooolps18_paper] [![Preprint][icooolps18_preprint]][icooolps18_pdf]


[codacy]: https://app.codacy.com/app/fniephaus/graalsqueak/dashboard
[codacy_coverage]: https://img.shields.io/codacy/coverage/9748bfe3726b48c8973e3808549f6d05.svg
[codacy_grade]: https://img.shields.io/codacy/grade/9748bfe3726b48c8973e3808549f6d05.svg
[dev]: ../../tree/dev
[eclipse_cs]: http://checkstyle.org/eclipse-cs/
[eclipse_downloads]: https://www.eclipse.org/downloads/
[ecoop]: https://2018.ecoop.org/
[graal]: https://github.com/oracle/graal
[graalsqueak]: bin/graalsqueak
[graalsqueak_image]: https://github.com/hpi-swa-lab/graalsqueak/releases/latest
[graalsqueak_launcher]: src/de.hpi.swa.graal.squeak.launcher/src/de/hpi/swa/graal/squeak/launcher/GraalSqueakLauncher.java
[graalvm]: http://www.graalvm.org/
[graalvm_download]: http://www.graalvm.org/downloads/
[icooolps18]: https://2018.ecoop.org/event/icooolps-2018-papers-graalsqueak-a-fast-smalltalk-bytecode-interpreter-written-in-an-ast-interpreter-framework
[icooolps18_doi]: https://img.shields.io/badge/doi-10.1145/3242947.3242948-blue.svg
[icooolps18_paper]: https://doi.org/10.1145/3242947.3242948
[icooolps18_pdf]: https://fniephaus.com/2018/icooolps18-graalsqueak.pdf
[icooolps18_preprint]: https://img.shields.io/badge/preprint-download-blue.svg
[issues]: ../../issues/new
[labsjdk]: http://www.oracle.com/technetwork/oracle-labs/program-languages/downloads/index.html
[master]: ../../tree/master
[mx]: https://github.com/graalvm/mx
[pull_request]: ../../compare/dev...
[squeak]: https://squeak.org
[squeak_downloads]: https://squeak.org/downloads/
[travis]: https://travis-ci.com/hpi-swa-lab/graalsqueak
[travis_badge]: https://travis-ci.com/hpi-swa-lab/graalsqueak.svg?token=7fqzGEv22MQpvpU7RhK5&branch=master
[vmmaker]: http://source.squeak.org/VMMaker/
