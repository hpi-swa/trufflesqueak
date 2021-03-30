world menu
registerInWorldMenu
	self environment at: #TheWorldMenu ifPresent: [ :class |
		class registerOpenCommand: (Array 
			with: 'Bouncing Atoms Simulation' 
			with: (Array
				with: self
				with: #openBouncingAtomsMorph)).
		class registerOpenCommand: (Array 
			with: 'Frame Rate Morph' 
			with: (Array
				with: self
				with: #openFrameRateMorph)) ].