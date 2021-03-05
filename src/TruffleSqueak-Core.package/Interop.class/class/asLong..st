primitives converting
asLong: anObject
	<primitive: 'primitiveAsLong' module: 'PolyglotPlugin'>
	^ (self fitsInLong: anObject)
		ifTrue: [ anObject interopAsLong ]
		ifFalse: [ self error: 'Object does not fit in long' ]