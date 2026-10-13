primitives
primitiveEval: aLanguage string: aString names: anArray arguments: anotherArray
	<primitive: 'primitiveEvalStringWithArguments' module: 'PolyglotPlugin'>
	anArray size ~= anotherArray size
		ifTrue: [ self error: 'size of names and arguments does not match' ].
	(Polyglot isPolyglotEvalAllowed: aLanguage)
		ifFalse: [ self error: 'Evaluating ', aLanguage, ' is not allowed' ].
	aLanguage == self languageIdSmalltalk
		ifTrue: [ ^ Compiler evaluate: aString logged: true ].
	PolyglotError signal