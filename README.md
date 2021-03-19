[![TruffleSqueak][ts_logo]](#)

[![Latest Release][ts_latest_badge]][ts_latest] [![Slack][graalvm_slack_badge]][graalvm_slack] [![Twitter][ts_twitter_badge]][ts_twitter] [![Build Status][ts_gh_action_badge]][ts_gh_action] [![Codacy][codacy_grade]][codacy] [![Coverage][codacy_coverage]][codacy] [![License][ts_license_badge]][ts_license]

A [Squeak/Smalltalk][squeak] VM and Polyglot Programming Environment for the [GraalVM][graalvm].


## Getting Started

1. Find the [latest TruffleSqueak release][ts_latest] and identify the
   supported version of GraalVM.
2. Download the corresponding [GraalVM][graalvm_download] for your platform.
3. Use the [GraalVM Updater][graalvm_updater] to install the TruffleSqueak
   component for your platform:

```bash
$GRAALVM_HOME/bin/gu \
  -C https://raw.githubusercontent.com/hpi-swa/trufflesqueak/master/gu-catalog.properties \
  install smalltalk
```

4. You should now be able to run TruffleSqueak:

```bash
$GRAALVM_HOME/bin/trufflesqueak
```


## Demos

<table>
  <tr align="center">
    <td width="50%"><a href="https://www.youtube.com/watch?v=rZiH9ub1aT4" title="TruffleSqueak: Polyglot Notebooks (Demo)"><img src="https://user-images.githubusercontent.com/2368856/90332594-6c2ead00-dfbe-11ea-8e92-f3136ad6ca05.png" alt="TruffleSqueak: Polyglot Notebooks (Demo)" /><br>TruffleSqueak: Polyglot Notebooks</a></td>
    <td width="50%"><a href="https://www.youtube.com/watch?v=_rnOW3ZMYQ4" title="TruffleSqueak: Workspace, Inspector, and Explorer (Demo)"><img src="https://user-images.githubusercontent.com/2368856/90333124-4f48a880-dfc3-11ea-8962-edc866a8d21a.png" alt="TruffleSqueak: Workspace, Inspector, and Explorer (Demo)" /><br>TruffleSqueak: Workspace, Inspector, and Explorer</a></td>
  </tr>
  <tr align="center">
    <td width="50%"><a href="https://twitter.com/fniephaus/status/1264839969115340800" title="TruffleSqueak: Live Plots with ggplot2 (Demo)"><img src="https://user-images.githubusercontent.com/2368856/90331920-b319a400-dfb8-11ea-877d-e0505c8adf20.png" alt="TruffleSqueak: Live Plots with ggplot2 (Demo)" /><br>TruffleSqueak: Live Plots with ggplot2</a></td>
    <td width="50%"><a href="https://www.youtube.com/watch?v=If7xNBYA0Bk" title="TruffleSqueak: Polyglot User Interfaces (Demo)"><img src="https://user-images.githubusercontent.com/2368856/90332427-0d1c6880-dfbd-11ea-9bfc-bc9927784437.png" alt="TruffleSqueak: Polyglot User Interfaces (Demo)" /><br>TruffleSqueak: Polyglot User Interfaces</a></td>
  </tr>
  <tr align="center">
    <td width="50%"><a href="https://www.youtube.com/watch?v=MxYoc6chBlg" title="TruffleSqueak: Polyglot Code Editor (Demo)"><img src="https://user-images.githubusercontent.com/2368856/90332428-10175900-dfbd-11ea-85b0-1b04d735827a.png" alt="TruffleSqueak: Polyglot Code Editor (Demo)" /><br>TruffleSqueak: Polyglot Code Editor</a></td>
    <td width="50%"><a href="https://www.youtube.com/watch?v=W7p-W9VAbQU" title="TruffleSqueak: Polyglot Code Finder (Demo)"><img src="https://user-images.githubusercontent.com/2368856/90332430-10afef80-dfbd-11ea-8448-b04e7a222f78.png" alt="TruffleSqueak: Polyglot Code Finder (Demo)" /><br>TruffleSqueak: Polyglot Code Finder</a></td>
  </tr>
  <tr align="center">
    <td width="50%"><a href="https://www.youtube.com/watch?v=wuGVyzUsEqE" title="TruffleSqueak vs. OpenSmalltalkVM: Bouncing Atoms"><img src="https://user-images.githubusercontent.com/2368856/90332179-350acc80-dfbb-11ea-803b-bdeb28577ef9.png" alt="TruffleSqueak vs. OpenSmalltalkVM: Bouncing Atoms" /><br>TruffleSqueak vs. OpenSmalltalkVM: Bouncing Atoms</a></td>
    <td width="50%"><a href="https://twitter.com/fniephaus/status/1363499071575642112" title="TruffleSqueak: CallTargetBrowser (Demo)"><img src="https://user-images.githubusercontent.com/2368856/108628591-24e91380-745c-11eb-9ff5-030a2b10dc35.png" alt="TruffleSqueak: CallTargetBrowser (Demo)" /><br>TruffleSqueak: CallTargetBrowser</a></td>
  </tr>
  <tr align="center">
    <td width="50%"><a href="https://twitter.com/fniephaus/status/1075807587491291137" title="TruffleSqueak: Polyglot Workspace (Demo)"><img src="https://user-images.githubusercontent.com/2368856/90332290-e4e03a00-dfbb-11ea-92e5-55bded783c8e.png" alt="TruffleSqueak: Polyglot Workspace (Demo)" /><br>TruffleSqueak: Polyglot Workspace</a></td>
    <td width="50%"></td>
  </tr>
</table>


## Talks

<table>
  <tr align="center">
    <td width="50%"><a href="https://www.youtube.com/watch?v=9SyOVmAqqLg" title="Polyglot Programming with TruffleSqueak and GraalVM (~21min, HPI Symposium at SAP, 2021)"><img src="https://user-images.githubusercontent.com/2368856/111785173-8d40de80-88bc-11eb-82e8-bff9008638ad.png" alt="Polyglot Programming with TruffleSqueak and GraalVM (~21min, HPI Symposium at SAP, 2021)" /><br>Polyglot Programming with TruffleSqueak and GraalVM<br>(~21min, HPI Symposium at SAP, 2021)</a></td>
    <td width="50%"><a href="https://www.youtube.com/watch?v=9SyOVmAqqLg" title="Some Smalltalk with Fabio Niephaus and Thomas Würthinger (~1h10min, Live on Twitch, 2021)"><img src="https://user-images.githubusercontent.com/2368856/111808110-f849df80-88d3-11eb-8c86-3d07cceae296.png" alt="Some Smalltalk with Fabio Niephaus and Thomas Würthinger (~1h10min, Live on Twitch, 2021)" /><br>Some Smalltalk with Fabio Niephaus and Thomas Würthinger<br>(~1h10min, Live on Twitch, 2021)</a></td>
  </tr>
  <tr align="center">
    <td width="50%"><a href="https://www.youtube.com/watch?v=FAk3Ec8hmzk" title="Polyglot Notebooks with Squeak/Smalltalk on the GraalVM (~28min, ESUG 2019)"><img src="https://user-images.githubusercontent.com/2368856/111785181-903bcf00-88bc-11eb-8722-7ca7d0558a22.png" alt="Polyglot Notebooks with Squeak/Smalltalk on the GraalVM (~28min, ESUG 2019)" /><br>Polyglot Notebooks with Squeak/Smalltalk on the GraalVM<br>(~28min, ESUG 2019)</a></td>
  </tr>
</table>


## Community Support

If you have a question, need some help, or want to discuss a new feature, feel
free to open an [issue][ts_issues] or join the `#trufflesqueak` channel on the
[GraalVM Slack][graalvm_slack].


## Documentation

Documentation is available in [`docs/`][ts_docs].


## Development

Active development is done in the [`master` branch][ts_master].
Please feel free to open a [pull request][pull_request] if you'd like to
contribute a bug-fix, documentation, or a new feature.
For more information, for example on how to build TruffleSqueak from source or
set up a development environment, please refer to the
[development.md][ts_dev_docs].


## Contributing

Please [report any issues here on GitHub][ts_issues] and open
[pull requests][pull_request] if you'd like to contribute code or documentation.


## Publications

*To cite this work, please use [the GraalSqueak paper presented at MPLR'19][mplr19_paper].*

### 2020
- Fabio Niephaus, Patrick Rein, Jakob Edding, Jonas Hering, Bastian König, Kolya
Opahle, Nico Scordialo, and Robert Hirschfeld. *Example-based Live Programming
for Everyone: Building Language-agnostic Tools for Live Programming With LSP and
GraalVM*. In Proceedings of [the ACM Symposium for New Ideas, New Paradigms, and
Reflections on Everything to do with Programming and Software (Onward!)
2020][onward20], co-located with the Conference on Object-oriented Programming,
Systems, Languages, and Applications (OOPSLA), pages 108-124, Chicago, United
States, November 17-18, 2020, ACM DL.  
[![doi][onward20_doi]][onward20_paper] [![Preprint][preprint]][onward20_pdf]
- Jan Ehmueller, Alexander Riese, Hendrik Tjabben, Fabio Niephaus, and Robert
Hirschfeld. *Polyglot Code Finder*. In Proceedings of [the Programming
Experience 2020 (PX/20) Workshop][px20], companion volume to International
Conference on the Art, Science, and Engineering of Programming (‹Programming›),
co-located with the International Conference on the Art, Science, and Engineering
of Programming (‹Programming›), 6 pages, Genova, Italy, April 1, 2019, ACM DL.  
[![doi][px20_doi]][px20_paper] [![Preprint][preprint]][px20_pdf]
- Alexander Riese, Fabio Niephaus, Tim Felgentreff, and Robert Hirschfeld.
*User-defined Interface Mappings for the GraalVM*. In Proceedings of [the
Interconnecting Code Workshop (ICW) 2020][icw20], companion volume to the
International Conference on the Art, Science, and Engineering of Programming
(‹Programming›), co-located with the International Conference on the Art,
Science, and Engineering of Programming (‹Programming›), pages 19-22, Porto,
Portugal, March 23, 2020, ACM DL.  
[![doi][icw20_doi]][icw20_paper] [![Preprint][preprint]][icw20_pdf]

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

TruffleSqueak is released under the [MIT license][ts_license].


[codacy]: https://app.codacy.com/gh/hpi-swa/trufflesqueak/dashboard
[codacy_coverage]: https://img.shields.io/codacy/coverage/104b3300600346789d604fd269219efe.svg
[codacy_grade]: https://img.shields.io/codacy/grade/104b3300600346789d604fd269219efe.svg
[dls19]: https://conf.researchr.org/home/dls-2019
[dls19_doi]: https://img.shields.io/badge/doi-10.1145/3359619.3359746-blue.svg
[dls19_paper]: https://doi.org/10.1145/3359619.3359746
[dls19_pdf]: https://www.hpi.uni-potsdam.de/hirschfeld/publications/media/StolpeFelgentreffHumerNiephausHirschfeld_2019_LanguageIndependentDevelopmentEnvironmentSupportForDynamicRuntimes_AcmDL.pdf
[eclipse_cs]: https://checkstyle.org/eclipse-cs/
[eclipse_downloads]: https://www.eclipse.org/downloads/
[github_releases]: https://help.github.com/en/github/administering-a-repository/about-releases
[graal]: https://github.com/oracle/graal
[graalvm]: https://www.graalvm.org/
[graalvm_blog]: https://medium.com/graalvm
[graalvm_download]: https://www.graalvm.org/downloads/
[graalvm_updater]: https://www.graalvm.org/docs/reference-manual/install-components/
[graalvm_slack]: https://www.graalvm.org/slack-invitation/
[graalvm_slack_badge]: https://img.shields.io/badge/slack-%23trufflesqueak-active
[icooolps18]: https://2018.ecoop.org/event/icooolps-2018-papers-graalsqueak-a-fast-smalltalk-bytecode-interpreter-written-in-an-ast-interpreter-framework
[icooolps18_doi]: https://img.shields.io/badge/doi-10.1145/3242947.3242948-blue.svg
[icooolps18_paper]: https://doi.org/10.1145/3242947.3242948
[icooolps18_pdf]: https://fniephaus.com/2018/icooolps18-graalsqueak.pdf
[icw20]: https://2020.programming-conference.org/home/icw-2020
[icw20_doi]: https://img.shields.io/badge/doi-10.1145/3397537.3399577-blue.svg
[icw20_paper]: https://dl.acm.org/doi/10.1145/3397537.3399577
[icw20_pdf]: https://www.hpi.uni-potsdam.de/hirschfeld/publications/media/RieseNiephausFelgentreffHirschfeld_2020_UserDefinedInterfaceMappingsForTheGraalVM_AcmDL.pdf
[javaadvent19]: https://www.javaadvent.com/2019/12/smalltalk-with-the-graalvm.html
[morevms19]: https://2019.programming-conference.org/track/MoreVMs-2019
[morevms19_doi]: https://img.shields.io/badge/doi-10.1145/3328433.3328440-blue.svg
[morevms19_paper]: https://doi.org/10.1145/3328433.3328440
[morevms19_pdf]: https://fniephaus.com/2019/morevms19-efficient-activation-records.pdf
[mplr19]: https://conf.researchr.org/home/mplr-2019
[mplr19_doi]: https://img.shields.io/badge/doi-10.1145/3357390.3361024-blue.svg
[mplr19_paper]: https://doi.org/10.1145/3357390.3361024
[mplr19_pdf]: https://fniephaus.com/2019/mplr19-graalsqueak.pdf
[mx]: https://github.com/graalvm/mx
[onward20]: https://2020.splashcon.org/track/splash-2020-Onward-papers
[onward20_doi]: https://img.shields.io/badge/doi-10.1145/3426428.3426919-blue.svg
[onward20_paper]: https://doi.org/10.1145/3426428.3426919
[onward20_pdf]: https://fniephaus.com/2020/onward20-live-programming.pdf
[pp19_post]: https://medium.com/p/3fd06ffa59d2/
[preprint]: https://img.shields.io/badge/preprint-download-blue.svg
[pull_request]: https://help.github.com/en/github/collaborating-with-issues-and-pull-requests/creating-a-pull-request
[px19]: https://2019.programming-conference.org/track/px-2019-papers
[px19_doi]: https://img.shields.io/badge/doi-10.1145/3328433.3328434-blue.svg
[px19_paper]: https://doi.org/10.1145/3328433.3328434
[px19_pdf]: https://fniephaus.com/2019/px19-polyglot-notebooks.pdf
[px20]: https://2020.programming-conference.org/track/px-2020-papers
[px20_doi]: https://img.shields.io/badge/doi-10.1145/3397537.3397559-blue.svg
[px20_paper]: https://doi.org/10.1145/3397537.3397559
[px20_pdf]: https://www.hpi.uni-potsdam.de/hirschfeld/publications/media/EhmuellerRieseTjabbenNiephausHirschfeld_2020_PolyglotCodeFinder_AcmDL.pdf
[sdr19]: https://2019.programming-conference.org/track/sdr-2019-papers
[sdr19_doi]: https://img.shields.io/badge/doi-10.1145/3328433.3338056-blue.svg
[sdr19_paper]: https://doi.org/10.1145/3328433.3338056
[sdr19_pdf]: https://www.hpi.uni-potsdam.de/hirschfeld/publications/media/PapeFelgentreffNiephausHirschfeld_2019_LetThemFailTowardsVmBuiltInBehaviorThatFallsBackToTheProgram_AcmDL.pdf
[squeak]: https://squeak.org
[squeak_downloads]: https://squeak.org/downloads/
[ts_docs]: https://github.com/hpi-swa/trufflesqueak/tree/master/docs
[ts_dev_docs]: https://github.com/hpi-swa/trufflesqueak/blob/master/docs/development.md
[ts_gh_action]: https://github.com/hpi-swa/trufflesqueak/actions
[ts_gh_action_badge]: https://img.shields.io/github/workflow/status/hpi-swa/trufflesqueak/CI
[ts_issues]: https://github.com/hpi-swa/trufflesqueak/issues/new
[ts_latest]: https://github.com/hpi-swa/trufflesqueak/releases/latest
[ts_latest_badge]: https://img.shields.io/github/v/release/hpi-swa/trufflesqueak
[ts_launcher]: https://github.com/hpi-swa/trufflesqueak/blob/master/src/de.hpi.swa.trufflesqueak.launcher/src/de/hpi/swa/trufflesqueak/launcher/TruffleSqueakLauncher.java
[ts_license]: https://github.com/hpi-swa/trufflesqueak/blob/master/LICENSE
[ts_license_badge]: https://img.shields.io/github/license/hpi-swa/trufflesqueak
[ts_logo]: https://user-images.githubusercontent.com/2368856/83736775-67f72280-a652-11ea-9785-35c2a688c0fc.png
[ts_master]: https://github.com/hpi-swa/trufflesqueak/tree/master
[ts_twitter]: https://twitter.com/TruffleSqueak
[ts_twitter_badge]: https://img.shields.io/badge/twitter-%40TruffleSqueak-active
