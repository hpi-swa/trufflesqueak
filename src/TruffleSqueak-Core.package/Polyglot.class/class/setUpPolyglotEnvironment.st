system startup
setUpPolyglotEnvironment
	self isSupportedByVM ifTrue: [ ForeignObject register ].
	self primitiveIsPolyglotBindingsAccessAllowed
		ifTrue: [ self export: 'bindings' value: Polyglot bindings ]