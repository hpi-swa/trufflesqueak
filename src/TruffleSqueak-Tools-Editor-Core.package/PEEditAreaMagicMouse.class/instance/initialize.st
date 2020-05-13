initialize-release
initialize

	super initialize.
	browser := (Smalltalk at: #MMBrowserMorph) new
		layoutFrame: (LayoutFrame fractions: (0@0 extent: 1@1))
		yourself