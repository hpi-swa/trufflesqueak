image creation
setUpUIBenchmark
	Preferences setFlag: #higherPerformance toValue: true.
	DisplayScreen
		fullScreenOff;
		setNewScreenSize: 1024 @ 768.

	World submorphs
		select: [ :ea | ea isKindOf: SimpleButtonMorph ]
		thenDo: [ :ea | ea abandon ].
	ReleaseBuilder deleteAllWindows.
	
	World dockingBars do: [ :b | b addMorph: UIBenchmarkInfoMorph new inFrontOf:
		(b submorphs detect: [:ea | ea isKindOf: AlignmentMorph]) ].

	BouncingAtomsMorph new
		addAtoms: 70; "100 atoms"
		extent: 800@600;
		center: World center;
		openInWorld.

	Smalltalk garbageCollect.