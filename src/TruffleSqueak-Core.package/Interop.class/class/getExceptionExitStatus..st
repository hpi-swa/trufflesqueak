primitives exceptions
getExceptionExitStatus: anObject
	<primitive: 'primitiveGetExceptionExitStatus' module: 'PolyglotPlugin'>
	((self isException: anObject) and: [ (self getExceptionType: anObject) = 'EXIT' ])
		ifFalse: [ self error: 'Object is not an exit exception' ]
		ifTrue: [ self error: 'Unexpected error' ]