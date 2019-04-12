primitives
primitiveAsLong
	<primitive: 'primitiveAsLong' module: 'PolyglotPlugin'>
	self primitiveFitsInLong
		ifFalse: [ self error: 'Object does not fit in long' ]
		ifTrue: [ self error: 'Unexpected error' ]