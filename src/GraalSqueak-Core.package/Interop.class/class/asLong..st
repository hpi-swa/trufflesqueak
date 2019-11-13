primitives
asLong: anObject
	<primitive: 'primitiveAsLong' module: 'PolyglotPlugin'>
	(self fitsInLong: anObject)
		ifFalse: [ self error: 'Object does not fit in long' ]
		ifTrue: [ self error: 'Unexpected error' ]