primitives
throwException: anObject
	<primitive: 'primitiveThrowException' module: 'PolyglotPlugin'>
	(self isException: anObject)
		ifFalse: [ self error: 'Object is not an exception and cannot be thrown' ]
		ifTrue: [ self error: 'Unexpected error' ]