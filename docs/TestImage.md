# Test Image Creation


```smalltalk
| manifest load |
manifest := #(
    ('http://source.squeak.org/FFI'          1 ('FFI-Pools' 'FFI-Kernel' 'FFI-Tests'))
    ('http://source.squeak.org/VMMaker'      6 ('Balloon-Engine-Pools' 'VMMaker.oscog'))
    ('http://www.squeaksource.com/OSProcess' 4 ('OSProcess'))
).

load := (manifest collect: [:tuple |
    [:path :order :packages| | repository |
    repository := MCHttpRepository
        location: path
        user: 'squeak'
        password: 'squeak'.
    MCRepositoryGroup default addRepository: repository.
    {repository. order. packages}] valueWithArguments: tuple]).

    load := (manifest collect:
        [:tuple|
        [:path :order :packages| | repository |
        repository := MCHttpRepository
                        location: path
                        user: 'squeak'
                        password: 'squeak'.
        MCRepositoryGroup default addRepository: repository.
        {repository. order. packages}] valueWithArguments: tuple])
    sort: [:a :b| a second <= b second].
load do: [:tuple |
    [:repository :order :packages|
    packages do:
        [:package| | latestVersion |
        "We need to filter-out branches of unbranched packages."
        latestVersion := (repository versionNamesForPackageNamed: package)
            detect: [:versionName| (versionName at: package size + 1) = $-].
        [| version |
        version := (
            (MCCacheRepository default includesVersionNamed: latestVersion)
                ifTrue: [MCCacheRepository default]
                ifFalse: [repository]) versionNamed: latestVersion.
         version load.
         version workingCopy repositoryGroup addRepository: repository]
            on: Warning
            do: [:ex| ex resume]]]
        valueWithArguments: tuple].

"Patch VMMaker classes for GraalSqueak"
Utilities authorInitials: 'GraalSqueak'.
(Smalltalk at: #InterpreterProxy) compile: 'stringOf: oop
    ^ self cStringOrNullFor: oop'.
(Smalltalk at: #BitBltSimulation) compile: 'assert: aBlock "disable #assert:"'.
(Smalltalk at: #ThisOSProcess) class compile: 'startUp: resuming "disable #startUp:"'.

"Disable performance killers"
World setAsBackground: Color black lighter.
Morph useSoftDropShadow: false.

SystemWindow gradientWindow: false.
DialogWindow gradientDialog: false.
MenuMorph gradientMenu: false.
PluggableButtonMorph gradientButton: false.
ScrollBar gradientScrollBar: false.

Morph indicateKeyboardFocus: false.
```
