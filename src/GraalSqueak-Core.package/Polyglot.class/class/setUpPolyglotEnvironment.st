system startup
setUpPolyglotEnvironment
	self primitiveIsPolyglotEvalAllowed
		ifTrue: [ ForeignObject register ].
	self primitiveIsPolyglotBindingsAccessAllowed
		ifTrue: [ self export: 'bindings' value: Polyglot bindings ]