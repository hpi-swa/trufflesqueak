primitives exceptions
getExceptionMessage: anObject
	<primitive: 'primitiveGetExceptionMessage' module: 'PolyglotPlugin'>
	^ (self hasExceptionMessage: anObject)
		ifTrue: [ anObject interopGetExceptionMessage ]
		ifFalse: [ self error: 'Exception has no message' ]