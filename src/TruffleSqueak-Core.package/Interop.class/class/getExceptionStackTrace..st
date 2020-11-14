primitives exceptions
getExceptionStackTrace: anObject
	<primitive: 'primitiveGetExceptionStackTrace' module: 'PolyglotPlugin'>
	(self hasExceptionStackTrace: anObject)
		ifFalse: [ self error: 'Exception has no stack trace' ]
		ifTrue: [ self error: 'Unexpected error' ]