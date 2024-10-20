primitives
primitiveLastError
	<primitive: 'primitiveGetLastError' module: 'PolyglotPlugin'>
	(self primitiveIsPolyglotEvalAllowed: Polyglot languageIdSmalltalk)
		ifTrue: [ self primitiveFailed ]
		ifFalse: [ self error: 'Polyglot API is not available' ]