system startup
setUpPolyglotEnvironment
	(self primitiveIsPolyglotEvalAllowed: Polyglot languageIdSmalltalk)
		ifTrue: [ ForeignObject register ].
	self primitiveIsPolyglotBindingsAccessAllowed
		ifTrue: [ self export: 'bindings' value: Polyglot bindings ]