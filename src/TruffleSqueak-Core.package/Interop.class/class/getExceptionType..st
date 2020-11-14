primitives exceptions
getExceptionType: anObject
	<primitive: 'primitiveGetExceptionType' module: 'PolyglotPlugin'>
	(self isException: anObject)
		ifFalse: [ self error: 'Object is not an exception' ]
		ifTrue: [ self error: 'Unexpected error' ]