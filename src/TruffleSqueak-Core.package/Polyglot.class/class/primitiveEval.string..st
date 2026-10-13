primitives
primitiveEval: aLanguage string: aString
	<primitive: 'primitiveEvalString' module: 'PolyglotPlugin'>
	aLanguage == self languageIdSmalltalk
		ifTrue: [ ^ Compiler evaluate: aString logged: true ].
	PolyglotError signal