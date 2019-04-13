world menu
registerInWorldMenu
	self environment at: #TheWorldMenu ifPresent: [ :class |
		class registerOpenCommand: (Array 
			with: 'Polyglot Workspace' 
			with: (Array
				with: self
				with: #open)) ].