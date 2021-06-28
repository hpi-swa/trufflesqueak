primitives
primitiveEval: aLanguage string: aString names: anArray arguments: anotherArray
	<primitive: 'primitiveEvalStringWithArguments' module: 'PolyglotPlugin'>
	anArray size ~= anotherArray size
		ifTrue: [ self error: 'size of names and arguments does not match' ].
	PolyglotError signal