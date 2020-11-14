primitives exceptions
getExceptionCause: anObject
	<primitive: 'primitiveGetExceptionCause' module: 'PolyglotPlugin'>
	(self hasExceptionCause: anObject)
		ifFalse: [ self error: 'Exception has no cause' ]
		ifTrue: [ self error: 'Unexpected error' ]