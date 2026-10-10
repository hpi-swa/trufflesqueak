class initialization
initialize
	super initialize.
	Smalltalk addToStartUpList: self.
	self setUpPolyglotEnvironment