world menu
registerInWorldMenu
	self environment at: #TheWorldMenu ifPresent: [ :class |
		class registerOpenCommand: (Array 
			with: 'Polyglot Notebook' 
			with: (Array
				with: self
				with: #open)) ].