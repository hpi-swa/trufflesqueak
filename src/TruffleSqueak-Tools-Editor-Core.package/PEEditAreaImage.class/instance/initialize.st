initialize-release
initialize

	super initialize.
	imageMorph := PESketchMorph new
		keepAspectRatio: true;
		layoutFrame: (LayoutFrame fractions: (0@0 extent: 1@1))
		yourself