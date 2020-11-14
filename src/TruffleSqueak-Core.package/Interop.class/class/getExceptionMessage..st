primitives exceptions
getExceptionMessage: anObject
	<primitive: 'primitiveGetExceptionMessage' module: 'PolyglotPlugin'>
	(self hasExceptionMessage: anObject)
		ifFalse: [ self error: 'Exception has no message' ]
		ifTrue: [ self error: 'Unexpected error' ]