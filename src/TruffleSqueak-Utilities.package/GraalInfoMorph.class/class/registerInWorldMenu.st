world menu
registerInWorldMenu
	self environment at: #TheWorldMenu ifPresent: [ :class |
		class registerOpenCommand: (Array 
			with: 'Graal Info Morph'
			with: (Array
				with: self
				with: #openInHand)) ].