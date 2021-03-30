world menu
registerInWorldMenu
	self environment at: #TheWorldMenu ifPresent: [ :class |
		class registerOpenCommand: (Array 
			with: 'CallTarget Browser' 
			with: (Array
				with: self
				with: #open)) ].