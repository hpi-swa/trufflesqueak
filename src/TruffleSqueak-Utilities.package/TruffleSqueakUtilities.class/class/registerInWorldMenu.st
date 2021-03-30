world menu
registerInWorldMenu
	self environment at: #TheWorldMenu ifPresent: [ :class |
		class
			registerOpenCommand: (Array 
				with: 'Bouncing Atoms Simulation' 
				with: (Array
					with: self
					with: #openBouncingAtomsMorph));
			registerOpenCommand: (Array 
				with: 'Frame Rate Morph' 
				with: (Array
					with: self
					with: #openFrameRateMorph));
			registerOpenCommand: (Array 
				with: 'Transcript' 
				with: (Array
					with: Transcript
					with: #open));
			registerOpenCommand: (Array 
				with: 'System Browser' 
				with: (Array
					with: Browser
					with: #open));
			registerOpenCommand: (Array 
				with: 'Test Runner' 
				with: (Array
					with: self
					with: #openTruffleSqueakTests)) ]
