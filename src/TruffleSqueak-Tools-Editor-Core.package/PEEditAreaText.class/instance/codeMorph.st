accessing
codeMorph

	^ self myDependents first findDeepSubmorphThat: [:m | m class = PEPluggableCodeMorph] ifAbsent: [self error: 'no code morph'].