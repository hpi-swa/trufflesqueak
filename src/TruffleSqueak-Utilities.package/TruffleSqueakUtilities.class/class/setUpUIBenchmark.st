image creation
setUpUIBenchmark
	| isDefault bam |
	isDefault := (UIManager default chooseFrom: #('600x400 (default)' '1024x768 (large)')
		lines: #()
		title: 'Choose the resolution of the benchmark') = 1.

	Preferences setFlag: #higherPerformance toValue: true.
	DisplayScreen
		fullScreenOff;
		setNewScreenSize: (isDefault ifTrue: [ 600@400 ] ifFalse: [ 1024@768 ]).

	self closeAllWindowsAndMorphs.
	
	World dockingBars do: [ :b | b addMorph: UIBenchmarkInfoMorph new inFrontOf:
		(b submorphs detect: [:ea | ea isKindOf: AlignmentMorph]) ].

	bam := BouncingAtomsMorph new.
	isDefault ifFalse: [ bam
						addAtoms: 70; "100 atoms"
						extent: 800@600 ].
	bam
		center: World center;
		openInWorld.

	Smalltalk garbageCollect.