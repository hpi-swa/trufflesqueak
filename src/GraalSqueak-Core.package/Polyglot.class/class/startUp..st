system startup
startUp: resuming
	self primitiveIsPolyglotBindingsAccessAllowed
		ifTrue: [ self export: 'bindings' value: Polyglot bindings ]