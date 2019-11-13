primitives
asDouble: anObject
	<primitive: 'primitiveAsDouble' module: 'PolyglotPlugin'>
	(self fitsInDouble: anObject)
		ifFalse: [ self error: 'Object does not fit in double' ]
		ifTrue: [ self error: 'Unexpected error' ]