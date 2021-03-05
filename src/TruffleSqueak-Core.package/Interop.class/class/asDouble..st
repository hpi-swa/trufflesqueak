primitives converting
asDouble: anObject
	<primitive: 'primitiveAsDouble' module: 'PolyglotPlugin'>
	^ (self fitsInDouble: anObject)
		ifTrue: [ anObject interopAsDouble ]
		ifFalse: [ self error: 'Object does not fit in double' ]