primitives exceptions
isExceptionIncompleteSource: anObject
	<primitive: 'primitiveIsExceptionIncompleteSource' module: 'PolyglotPlugin'>
	((self isException: anObject) and: [ (self getExceptionType: anObject) = 'PARSE_ERROR' ])
		ifFalse: [ self error: 'Object is not a parse error' ]
		ifTrue: [ self error: 'Unexpected error' ]